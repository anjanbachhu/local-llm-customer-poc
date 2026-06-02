package com.poc.llm.service.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.llm.config.LlmProperties;
import com.poc.llm.model.ExtractionResult;
import com.poc.llm.model.SearchCriteria;
import de.kherud.llama.InferenceParameters;
import de.kherud.llama.LlamaModel;
import de.kherud.llama.ModelParameters;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Local LLM implementation backed by <a href="https://github.com/kherud/java-llama.cpp">java-llama.cpp</a>.
 *
 * <p>Loads a quantised GGUF model (Qwen2.5-0.5B-Instruct by default) from the
 * local {@code models/} directory and runs inference on the CPU. Only the user's
 * question is ever sent to the model; the model returns a small JSON filter which
 * is parsed into {@link SearchCriteria}. The customer records themselves never
 * leave the JVM, so the approach scales regardless of dataset size.
 *
 * <p>If the model file is missing, disabled, or returns unparseable output, the
 * service transparently falls back to {@link RuleBasedCriteriaExtractor} so the
 * application always remains usable.
 */
@Slf4j
@Service
public class LocalLlmService implements LlmService {

    private static final String SYSTEM_PROMPT = """
            You are a customer search assistant.
            Convert the user's request into a JSON filter object.

            Supported fields:
            - customerId (string)
            - name (string)
            - email (string)
            - city (string)
            - status (string: "Active" or "Inactive")
            - emailContains (string, for partial email matches such as "gmail")
            - missingEmail (boolean, true for customers without an email)
            - duplicates (boolean, true to find duplicate customers)

            Rules:
            - Return only valid JSON.
            - Do not include explanations.
            - Do not include markdown.
            - Do not invent fields.
            - Only include fields that the request actually mentions.

            Example request: Find active customers from London
            Example response: {"city": "London", "status": "Active"}""";

    private final LlmProperties properties;
    private final ObjectMapper objectMapper;
    private final RuleBasedCriteriaExtractor fallback;

    /** Loaded lazily in {@link #init()}; null when the model is unavailable. */
    private volatile LlamaModel model;

    public LocalLlmService(LlmProperties properties,
                           ObjectMapper objectMapper,
                           RuleBasedCriteriaExtractor fallback) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.fallback = fallback;
    }

    @PostConstruct
    void init() {
        if (!properties.isEnabled()) {
            log.warn("LLM disabled via configuration (app.llm.enabled=false). "
                    + "Using the rule-based extractor.");
            return;
        }
        Path modelFile = Path.of(properties.getModelPath());
        if (!Files.isRegularFile(modelFile)) {
            log.warn("Model file not found at '{}'. The app will run with the rule-based "
                    + "extractor until the GGUF model is downloaded. See README for instructions.",
                    modelFile.toAbsolutePath());
            return;
        }
        try {
            log.info("Loading local LLM '{}' from {} ...", properties.getModelName(),
                    modelFile.toAbsolutePath());
            ModelParameters params = new ModelParameters()
                    .setModel(modelFile.toString())
                    .setGpuLayers(properties.getGpuLayers());
            this.model = new LlamaModel(params);
            log.info("Local LLM loaded successfully.");
        } catch (Throwable t) {
            // Catch Throwable: native library loading can fail with UnsatisfiedLinkError.
            this.model = null;
            log.error("Failed to load local LLM; falling back to the rule-based extractor. Reason: {}",
                    t.getMessage(), t);
        }
    }

    @Override
    public ExtractionResult extractCriteria(String question) {
        if (model == null) {
            return new ExtractionResult(fallback.extract(question), "rule-based fallback", "");
        }
        String raw = "";
        try {
            String prompt = buildChatMlPrompt(question);
            InferenceParameters inferParams = new InferenceParameters(prompt)
                    .setTemperature(properties.getTemperature())
                    .setNPredict(properties.getMaxTokens())
                    .setStopStrings("<|im_end|>", "<|endoftext|>");

            raw = model.complete(inferParams);
            SearchCriteria criteria = parseCriteria(raw);
            if (criteria != null) {
                return new ExtractionResult(criteria, properties.getModelName(), raw.trim());
            }
            log.warn("Model output was not valid JSON; using rule-based fallback. Output: {}", raw);
        } catch (Throwable t) {
            log.error("Inference failed; using rule-based fallback. Reason: {}", t.getMessage(), t);
        }
        return new ExtractionResult(fallback.extract(question), "rule-based fallback (model output unparseable)", raw);
    }

    /**
     * Build a Qwen2.5 ChatML prompt. We format it explicitly (rather than relying
     * on the model's embedded chat template) so behaviour is identical across
     * library versions.
     */
    private String buildChatMlPrompt(String question) {
        return "<|im_start|>system\n" + SYSTEM_PROMPT + "<|im_end|>\n"
                + "<|im_start|>user\n" + question + "<|im_end|>\n"
                + "<|im_start|>assistant\n";
    }

    /**
     * Extract the first JSON object from the model's output and parse it.
     *
     * @return the parsed criteria, or null if no valid JSON object was found
     */
    private SearchCriteria parseCriteria(String raw) {
        if (raw == null) {
            return null;
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        String json = raw.substring(start, end + 1);
        try {
            return objectMapper.readValue(json, SearchCriteria.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isModelLoaded() {
        return model != null;
    }

    @Override
    public String modelName() {
        return properties.getModelName();
    }

    @PreDestroy
    void shutdown() {
        if (model != null) {
            try {
                model.close();
                log.info("Local LLM unloaded.");
            } catch (Exception e) {
                log.warn("Error while closing the model: {}", e.getMessage());
            }
        }
    }
}

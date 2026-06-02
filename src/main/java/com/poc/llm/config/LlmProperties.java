package com.poc.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for the {@code app.llm.*} configuration block.
 *
 * <p>Centralising these values means a larger model can be plugged in purely
 * through configuration (change {@link #modelPath}) without code changes.
 */
@Data
@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {

    /** Master switch; when false the model is never loaded and the rule-based extractor is used. */
    private boolean enabled = true;

    /** Path to the local GGUF model file (relative or absolute). */
    private String modelPath = "models/qwen2.5-0.5b-instruct-q4_k_m.gguf";

    /** Human-readable model name, surfaced in the UI. */
    private String modelName = "Qwen2.5-0.5B-Instruct";

    /** Context window size in tokens. */
    private int contextSize = 2048;

    /** Maximum number of tokens to generate per request. */
    private int maxTokens = 200;

    /** Sampling temperature; 0.0 keeps the JSON output deterministic. */
    private float temperature = 0.0f;

    /** Number of layers to offload to the GPU; 0 = CPU only. */
    private int gpuLayers = 0;

    /** Number of CPU threads used for inference. */
    private int threads = 4;
}

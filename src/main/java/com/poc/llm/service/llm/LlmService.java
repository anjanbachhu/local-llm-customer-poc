package com.poc.llm.service.llm;

import com.poc.llm.model.ExtractionResult;

/**
 * Abstraction over "turn a natural-language question into a structured filter".
 *
 * <p>Keeping this an interface means the concrete inference engine (llama.cpp
 * today, ONNX/DJL/a remote model tomorrow) can be swapped without touching the
 * controller or business logic.
 */
public interface LlmService {

    /**
     * Translate a natural-language question into structured search criteria.
     * Implementations must never throw for ordinary "bad model output"; they
     * should fall back to a best-effort result instead.
     *
     * @param question the user's question
     * @return the extracted criteria plus metadata about how it was produced
     */
    ExtractionResult extractCriteria(String question);

    /** @return true if the local model is loaded and ready for inference. */
    boolean isModelLoaded();

    /** @return the human-readable model name (for display in the UI). */
    String modelName();
}

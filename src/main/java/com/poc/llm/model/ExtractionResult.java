package com.poc.llm.model;

/**
 * Result of converting a natural-language question into a {@link SearchCriteria}.
 *
 * @param criteria  the structured filter to apply to the in-memory customers
 * @param engine    which component produced the criteria ("Qwen2.5-0.5B-Instruct"
 *                  or "rule-based fallback") — surfaced in the UI for transparency
 * @param rawOutput the raw text returned by the model (empty for the fallback);
 *                  useful for debugging prompt behaviour
 */
public record ExtractionResult(SearchCriteria criteria, String engine, String rawOutput) {
}

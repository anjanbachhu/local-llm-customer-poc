package com.poc.llm.dto;

import com.poc.llm.model.Customer;
import com.poc.llm.model.SearchCriteria;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Result of running a natural-language query against the in-memory customers. */
@Data
@Builder
public class QueryResponse {

    /** The original question asked by the user. */
    private String question;

    /** The structured filter the question was translated into. */
    private SearchCriteria criteria;

    /** Which engine produced the criteria (LLM vs rule-based fallback). */
    private String engine;

    /** Number of matching customers. */
    private int matchCount;

    /** The matching customer records. */
    private List<Customer> results;

    /** A short, human-friendly summary of the outcome. */
    private String answer;

    /** Raw model output (for transparency/debugging); may be empty. */
    private String rawModelOutput;
}

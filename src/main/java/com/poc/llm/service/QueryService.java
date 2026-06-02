package com.poc.llm.service;

import com.poc.llm.dto.QueryResponse;
import com.poc.llm.model.Customer;
import com.poc.llm.model.ExtractionResult;
import com.poc.llm.service.llm.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates the hybrid LLM + business-logic search:
 *
 * <ol>
 *   <li>send only the question to the LLM to obtain structured criteria;</li>
 *   <li>apply that criteria to the in-memory customers with plain Java;</li>
 *   <li>assemble a UI-friendly response.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryService {

    private final LlmService llmService;
    private final CustomerFilterService filterService;
    private final CustomerStore customerStore;

    public QueryResponse query(String question) {
        List<Customer> allCustomers = customerStore.getAll();

        // Step 2 + 3: the model converts the question into a structured filter.
        ExtractionResult extraction = llmService.extractCriteria(question);
        log.info("Question '{}' -> criteria {} (via {})",
                question, extraction.criteria(), extraction.engine());

        // Step 4 + 5: filter the records locally and return the matches.
        List<Customer> matches = filterService.filter(extraction.criteria(), allCustomers);

        return QueryResponse.builder()
                .question(question)
                .criteria(extraction.criteria())
                .engine(extraction.engine())
                .matchCount(matches.size())
                .results(matches)
                .answer(buildAnswer(question, matches.size(), allCustomers.size(), extraction.criteria().isEmpty()))
                .rawModelOutput(extraction.rawOutput())
                .build();
    }

    private String buildAnswer(String question, int matchCount, int total, boolean emptyCriteria) {
        if (total == 0) {
            return "No customers have been uploaded yet. Upload one or more JSON files first.";
        }
        if (emptyCriteria) {
            return "I couldn't derive a specific filter from your question, so all "
                    + total + " customer(s) are shown.";
        }
        if (matchCount == 0) {
            return "No customers matched your query.";
        }
        return "Found " + matchCount + " customer(s) matching your query "
                + "(out of " + total + " total).";
    }
}

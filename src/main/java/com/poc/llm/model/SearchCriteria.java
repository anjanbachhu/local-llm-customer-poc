package com.poc.llm.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * Structured filter produced by the LLM (or the rule-based fallback) from a
 * natural-language question. This is the <em>only</em> thing the model returns;
 * the actual customer records are never sent to the model.
 *
 * <p>The first five fields mirror the {@link Customer} model. The remaining
 * fields express richer intents ("email contains gmail", "missing email",
 * "duplicates") that cannot be captured by a plain equality filter.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchCriteria {

    private String customerId;
    private String name;
    private String email;
    private String city;
    private String status;

    /** Partial, case-insensitive email match (e.g. "gmail"). */
    private String emailContains;

    /** When true, return only customers that have no email address. */
    private boolean missingEmail;

    /** When true, return only customers that are duplicated (same email). */
    private boolean duplicates;

    /** @return true when no field is set, i.e. the filter would match everything. */
    public boolean isEmpty() {
        return !StringUtils.hasText(customerId)
                && !StringUtils.hasText(name)
                && !StringUtils.hasText(email)
                && !StringUtils.hasText(city)
                && !StringUtils.hasText(status)
                && !StringUtils.hasText(emailContains)
                && !missingEmail
                && !duplicates;
    }
}

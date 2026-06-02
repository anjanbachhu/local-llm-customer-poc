package com.poc.llm.service.llm;

import com.poc.llm.model.SearchCriteria;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic, dependency-free fallback that derives {@link SearchCriteria}
 * from a question using simple heuristics.
 *
 * <p>It is used in two situations:
 * <ul>
 *   <li>the local model file is not present / disabled, so the app still works;</li>
 *   <li>the model returns text that cannot be parsed into valid JSON.</li>
 * </ul>
 * It is intentionally lightweight — the LLM is the primary path.
 */
@Component
public class RuleBasedCriteriaExtractor {

    private static final Pattern CITY_PATTERN =
            Pattern.compile("\\b(?:from|in|based in|located in)\\s+([a-zA-Z][a-zA-Z .'-]*?)" +
                    "(?=\\s*(?:\\?|$|,|\\bwith\\b|\\bwhose\\b|\\bwho\\b|\\bthat\\b|\\bare\\b))",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern EMAIL_CONTAINS_PATTERN =
            Pattern.compile("email\\s+(?:contains|containing|with|has|includes)\\s+([a-zA-Z0-9.@_-]+)",
                    Pattern.CASE_INSENSITIVE);

    public SearchCriteria extract(String question) {
        SearchCriteria criteria = new SearchCriteria();
        if (question == null) {
            return criteria;
        }
        String q = question.toLowerCase(Locale.ROOT);

        // status
        if (q.contains("inactive")) {
            criteria.setStatus("Inactive");
        } else if (q.contains("active")) {
            criteria.setStatus("Active");
        }

        // duplicates
        if (q.contains("duplicate")) {
            criteria.setDuplicates(true);
        }

        // missing email
        if ((q.contains("missing") || q.contains("without") || q.contains("no ") || q.contains("blank"))
                && q.contains("email")) {
            criteria.setMissingEmail(true);
        }

        // email contains <token>  (e.g. "email contains gmail")
        Matcher emailMatcher = EMAIL_CONTAINS_PATTERN.matcher(question);
        if (emailMatcher.find()) {
            criteria.setEmailContains(emailMatcher.group(1).trim());
        } else if (q.contains("gmail")) {
            criteria.setEmailContains("gmail");
        } else if (q.contains("yahoo")) {
            criteria.setEmailContains("yahoo");
        }

        // city: "from London", "in Manchester"
        Matcher cityMatcher = CITY_PATTERN.matcher(question);
        if (cityMatcher.find()) {
            String city = cityMatcher.group(1).trim();
            if (!city.isEmpty()) {
                criteria.setCity(capitalize(city));
            }
        }

        return criteria;
    }

    private String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}

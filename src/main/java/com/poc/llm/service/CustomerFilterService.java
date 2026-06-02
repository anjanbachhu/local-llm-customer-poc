package com.poc.llm.service;

import com.poc.llm.model.Customer;
import com.poc.llm.model.SearchCriteria;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Applies a {@link SearchCriteria} to a list of customers using plain Java
 * business logic. This is deliberately kept separate from the LLM: the model
 * only produces the criteria, while all filtering happens here and therefore
 * scales independently of the number of records.
 */
@Service
public class CustomerFilterService {

    /**
     * Filter the supplied customers according to the criteria.
     *
     * @param criteria  the structured filter (an "empty" criteria matches all)
     * @param customers the full set of customers to filter
     * @return the matching customers
     */
    public List<Customer> filter(SearchCriteria criteria, List<Customer> customers) {
        if (criteria == null) {
            return customers;
        }

        List<Customer> base = criteria.isDuplicates() ? findDuplicates(customers) : customers;

        return base.stream()
                .filter(buildPredicate(criteria))
                .collect(Collectors.toList());
    }

    private Predicate<Customer> buildPredicate(SearchCriteria c) {
        return customer -> matchesEquals(customer.getCustomerId(), c.getCustomerId())
                && matchesContains(customer.getName(), c.getName())
                && matchesContains(customer.getEmail(), c.getEmail())
                && matchesContains(customer.getCity(), c.getCity())
                && matchesEqualsIgnoreCase(customer.getStatus(), c.getStatus())
                && matchesContains(customer.getEmail(), c.getEmailContains())
                && matchesMissingEmail(customer, c.isMissingEmail());
    }

    /** Exact match (trimmed) used for identifiers. */
    private boolean matchesEquals(String actual, String wanted) {
        if (!StringUtils.hasText(wanted)) {
            return true;
        }
        return actual != null && actual.trim().equalsIgnoreCase(wanted.trim());
    }

    private boolean matchesEqualsIgnoreCase(String actual, String wanted) {
        if (!StringUtils.hasText(wanted)) {
            return true;
        }
        return actual != null && actual.trim().equalsIgnoreCase(wanted.trim());
    }

    /** Case-insensitive substring match used for free-text fields. */
    private boolean matchesContains(String actual, String wanted) {
        if (!StringUtils.hasText(wanted)) {
            return true;
        }
        return actual != null
                && actual.toLowerCase(Locale.ROOT).contains(wanted.toLowerCase(Locale.ROOT).trim());
    }

    private boolean matchesMissingEmail(Customer customer, boolean wantMissing) {
        if (!wantMissing) {
            return true;
        }
        return !StringUtils.hasText(customer.getEmail());
    }

    /**
     * @return customers whose (non-blank, case-insensitive) email is shared by at
     *         least one other customer — i.e. duplicate records.
     */
    private List<Customer> findDuplicates(List<Customer> customers) {
        Map<String, Long> countsByEmail = customers.stream()
                .filter(c -> StringUtils.hasText(c.getEmail()))
                .collect(Collectors.groupingBy(
                        c -> c.getEmail().trim().toLowerCase(Locale.ROOT),
                        Collectors.counting()));

        return customers.stream()
                .filter(c -> StringUtils.hasText(c.getEmail()))
                .filter(c -> countsByEmail.getOrDefault(
                        c.getEmail().trim().toLowerCase(Locale.ROOT), 0L) > 1)
                .collect(Collectors.toList());
    }
}

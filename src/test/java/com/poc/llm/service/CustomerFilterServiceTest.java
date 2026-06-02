package com.poc.llm.service;

import com.poc.llm.model.Customer;
import com.poc.llm.model.SearchCriteria;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for the pure business-logic filtering (no Spring context, no model). */
class CustomerFilterServiceTest {

    private final CustomerFilterService service = new CustomerFilterService();

    private final List<Customer> customers = List.of(
            Customer.builder().customerId("1").name("John").email("john@example.com").city("London").status("Active").build(),
            Customer.builder().customerId("2").name("Aisha").email("aisha@gmail.com").city("Manchester").status("Active").build(),
            Customer.builder().customerId("3").name("Carlos").email("carlos@yahoo.com").city("London").status("Inactive").build(),
            Customer.builder().customerId("4").name("Mei").email("").city("Leeds").status("Active").build(),
            Customer.builder().customerId("5").name("Aisha2").email("aisha@gmail.com").city("Manchester").status("Active").build()
    );

    @Test
    void filtersByCityAndStatus() {
        SearchCriteria c = new SearchCriteria();
        c.setCity("London");
        c.setStatus("Active");
        List<Customer> result = service.filter(c, customers);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getName());
    }

    @Test
    void filtersByEmailContains() {
        SearchCriteria c = new SearchCriteria();
        c.setEmailContains("gmail");
        assertEquals(2, service.filter(c, customers).size());
    }

    @Test
    void filtersMissingEmail() {
        SearchCriteria c = new SearchCriteria();
        c.setMissingEmail(true);
        List<Customer> result = service.filter(c, customers);
        assertEquals(1, result.size());
        assertEquals("Mei", result.get(0).getName());
    }

    @Test
    void filtersDuplicatesByEmail() {
        SearchCriteria c = new SearchCriteria();
        c.setDuplicates(true);
        List<Customer> result = service.filter(c, customers);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(cu -> cu.getEmail().equals("aisha@gmail.com")));
    }

    @Test
    void emptyCriteriaMatchesAll() {
        assertEquals(customers.size(), service.filter(new SearchCriteria(), customers).size());
    }
}

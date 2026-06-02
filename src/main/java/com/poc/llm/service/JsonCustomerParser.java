package com.poc.llm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.llm.exception.JsonParsingException;
import com.poc.llm.model.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Parses an uploaded JSON file into {@link Customer} records.
 *
 * <p>Accepts either a JSON array of customer objects or a single customer
 * object, so users can upload files in either shape.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JsonCustomerParser {

    private final ObjectMapper objectMapper;

    /**
     * Parse the given stream into a list of customers.
     *
     * @param input    the file content
     * @param fileName the original file name (used only for error messages)
     * @return the parsed customers (possibly empty, never null)
     * @throws JsonParsingException if the content is not valid customer JSON
     */
    public List<Customer> parse(InputStream input, String fileName) {
        try {
            JsonNode root = objectMapper.readTree(input);
            if (root == null || root.isNull() || root.isMissingNode()) {
                throw new JsonParsingException("File '" + fileName + "' is empty or not valid JSON.");
            }
            if (root.isArray()) {
                return objectMapper.convertValue(root, new TypeReference<List<Customer>>() {});
            }
            if (root.isObject()) {
                // A wrapper like { "customers": [ ... ] } is also supported.
                JsonNode customersNode = root.get("customers");
                if (customersNode != null && customersNode.isArray()) {
                    return objectMapper.convertValue(customersNode, new TypeReference<List<Customer>>() {});
                }
                Customer single = objectMapper.convertValue(root, Customer.class);
                return List.of(single);
            }
            throw new JsonParsingException("File '" + fileName
                    + "' must contain a customer object or an array of customer objects.");
        } catch (IOException e) {
            throw new JsonParsingException("Could not read JSON from '" + fileName + "': " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new JsonParsingException("Malformed customer data in '" + fileName + "': " + e.getMessage(), e);
        }
    }
}

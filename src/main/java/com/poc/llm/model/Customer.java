package com.poc.llm.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single customer record as parsed from an uploaded JSON file.
 *
 * <p>Unknown JSON fields are ignored so the parser is tolerant of extra
 * attributes that the POC does not model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Customer {

    private String customerId;
    private String name;
    private String email;
    private String city;
    private String status;
}

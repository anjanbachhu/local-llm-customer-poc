package com.poc.llm.controller;

import com.poc.llm.dto.QueryRequest;
import com.poc.llm.dto.QueryResponse;
import com.poc.llm.dto.UploadResponse;
import com.poc.llm.exception.JsonParsingException;
import com.poc.llm.model.Customer;
import com.poc.llm.service.CustomerStore;
import com.poc.llm.service.JsonCustomerParser;
import com.poc.llm.service.QueryService;
import com.poc.llm.service.llm.LlmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Serves the Thymeleaf UI and the JSON API used by the front-end:
 * <ul>
 *   <li>{@code GET  /}            – the single-page UI</li>
 *   <li>{@code POST /api/upload} – upload one or more JSON files</li>
 *   <li>{@code POST /api/query}  – ask a natural-language question</li>
 *   <li>{@code POST /api/reset}  – clear all in-memory data</li>
 *   <li>{@code GET  /api/status} – current counts and model state</li>
 * </ul>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CustomerController {

    private final JsonCustomerParser parser;
    private final CustomerStore customerStore;
    private final QueryService queryService;
    private final LlmService llmService;

    /** Render the main page, seeding it with the current model/data state. */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("modelName", llmService.modelName());
        model.addAttribute("modelLoaded", llmService.isModelLoaded());
        model.addAttribute("customerCount", customerStore.count());
        model.addAttribute("fileNames", customerStore.getFileNames());
        return "index";
    }

    @PostMapping(value = "/api/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public UploadResponse upload(@RequestParam("files") MultipartFile[] files) {
        List<String> errors = new ArrayList<>();
        int processed = 0;
        int added = 0;

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed.json";
            try {
                List<Customer> customers = parser.parse(file.getInputStream(), name);
                customerStore.addAll(name, customers);
                added += customers.size();
                processed++;
            } catch (JsonParsingException e) {
                errors.add(e.getMessage());
            } catch (IOException e) {
                errors.add("Could not read file '" + name + "': " + e.getMessage());
            }
        }

        return UploadResponse.builder()
                .filesProcessed(processed)
                .recordsAdded(added)
                .totalCustomers(customerStore.count())
                .fileNames(customerStore.getFileNames())
                .errors(errors)
                .build();
    }

    @PostMapping(value = "/api/query",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public QueryResponse query(@Valid @RequestBody QueryRequest request) {
        return queryService.query(request.getQuestion());
    }

    @PostMapping(value = "/api/reset", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public UploadResponse reset() {
        customerStore.clear();
        return UploadResponse.builder()
                .filesProcessed(0)
                .recordsAdded(0)
                .totalCustomers(0)
                .fileNames(customerStore.getFileNames())
                .errors(List.of())
                .build();
    }

    @GetMapping(value = "/api/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public UploadResponse status() {
        return UploadResponse.builder()
                .filesProcessed(0)
                .recordsAdded(0)
                .totalCustomers(customerStore.count())
                .fileNames(customerStore.getFileNames())
                .errors(List.of())
                .build();
    }
}

package com.poc.llm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Summary returned after uploading one or more JSON files. */
@Data
@Builder
public class UploadResponse {

    /** Number of files successfully processed in this request. */
    private int filesProcessed;

    /** Number of customer records added in this request. */
    private int recordsAdded;

    /** Total number of customers currently held in memory. */
    private int totalCustomers;

    /** All file names uploaded so far (across requests). */
    private List<String> fileNames;

    /** Per-file problems, if any (e.g. malformed JSON). */
    private List<String> errors;
}

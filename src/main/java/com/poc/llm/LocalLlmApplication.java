package com.poc.llm;

import com.poc.llm.config.LlmProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Entry point for the Local LLM Customer Search POC.
 *
 * <p>The application runs entirely offline: customer data lives in memory and a
 * locally hosted Qwen2.5-0.5B-Instruct model translates natural-language
 * questions into structured {@code SearchCriteria}. No external AI API is called.
 */
@SpringBootApplication
@EnableConfigurationProperties(LlmProperties.class)
public class LocalLlmApplication {

    public static void main(String[] args) {
        SpringApplication.run(LocalLlmApplication.class, args);
    }
}

package com.hazely.senusboard.jobs.ingestion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Holds OpenAI configuration used by the ingestion job.
 *
 * <p>The API key is loaded from {@code app.openai.api-key}. It must be supplied through an
 * environment variable or secret manager and must never be logged.</p>
 */
@Validated
@ConfigurationProperties(prefix = "app.openai")
public class OpenAiProperties {

    @NotBlank
    private String apiKey;

    @NotBlank
    private String model;

    @Positive
    private int maxOutputTokens = 20000;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(int maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }
}

package com.smarttool.logger.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.*;
import com.smarttool.logger.dto.ExplainResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Objects;

@Service
public class LlmService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExplainResponse explain(String userMessage) {

        Client client = Client.builder()
                .apiKey(apiKey)
                .build();

        String systemPrompt = "You are a senior Java engineer with 10 years of experience. " +
                "When given a Java question or stack trace, respond in this exact JSON format: " +
                "{\"explanation\": \"...\", \"rootCause\": \"...\", \"fix\": \"...\"}. " +
                "Be concise. Never add extra fields.";

        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(systemPrompt)))
                .temperature(0.0f)
                .maxOutputTokens(1024)
                .responseMimeType("application/json")
                .build();

        try {
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.5-flash",
                    userMessage,
                    config
            );

            System.out.println("Input tokens: " + response.usageMetadata()
                    .flatMap(GenerateContentResponseUsageMetadata::promptTokenCount)
                    .orElse(0));
            System.out.println("Output tokens: " + response.usageMetadata()
                    .flatMap(GenerateContentResponseUsageMetadata::candidatesTokenCount)
                    .orElse(0));

            String rawJson = Objects.requireNonNull(response.text()).trim();

            return objectMapper.readValue(rawJson, ExplainResponse.class);

        } catch (com.google.genai.errors.ClientException e) {
            // Covers 429 rate limit and 4xx errors
            if (e.getMessage().contains("429")) {
                System.err.println("Rate limit hit — back off and retry");
                throw new RuntimeException("LLM rate limit exceeded. Please try again later.");
            }
            System.err.println("LLM client error: " + e.getMessage());
            throw new RuntimeException("LLM request failed: " + e.getMessage());

        } catch (com.google.genai.errors.ApiException e) {
            // Covers 5xx server errors and timeouts
            System.err.println("LLM API error: " + e.getMessage());
            throw new RuntimeException("LLM service unavailable. Please try again later.");

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // Model returned something unparseable
            System.err.println("Failed to parse LLM response: " + e.getMessage());
            throw new RuntimeException("LLM returned unexpected output format.");

        } catch (NullPointerException e) {
            // response.text() was null — model returned empty response
            System.err.println("LLM returned null response");
            throw new RuntimeException("LLM returned an empty response.");
        }
    }

    public void stream(String userMessage, SseEmitter emitter) {

        Client client = Client.builder()
                .apiKey(apiKey)
                .build();

        String systemPrompt = "You are a senior Java engineer with 10 years of experience. " +
                "Answer clearly and concisely.";

        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(systemPrompt)))
                .temperature(0.0f)
                .maxOutputTokens(1024)
                .build();

        try {
            client.models.generateContentStream("gemini-2.5-flash", userMessage, config)
                    .forEach(chunk -> {
                        try {
                            String text = chunk.text();
                            if (text != null && !text.isEmpty()) {
                                emitter.send(SseEmitter.event().data(text));
                            }
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    });

            emitter.complete();

        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }
}

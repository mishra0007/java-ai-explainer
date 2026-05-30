package com.smarttool.logger.controller;

import com.smarttool.logger.dto.ExplainResponse;
import com.smarttool.logger.service.LlmService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class LlmController {

    private final LlmService llmService;

    public LlmController(LlmService llmService) {
        this.llmService = llmService;
    }

    @PostMapping("/explain")
    public ExplainResponse explain(@RequestBody(required = false) String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        return llmService.explain(message);
    }

    @GetMapping(value = "/explain/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String message) {
        SseEmitter emitter = new SseEmitter();

        if (message == null || message.isBlank()) {
            emitter.completeWithError(new RuntimeException("Message cannot be empty"));
            return emitter;
        }

        // Run in background thread — SSE is async
        new Thread(() -> {
            try {
                llmService.stream(message, emitter);
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleError(RuntimeException e) {
        return ResponseEntity
                .status(500)
                .body("{\"error\": \"" + e.getMessage() + "\"}");
    }
}

package com.comsenity.geecon.genai.coordinator.web;

import com.comsenity.geecon.genai.coordinator.agent.GeeconCoordinator;
import com.comsenity.geecon.genai.coordinator.guardrail.GuardrailViolationException;
import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final GeeconCoordinator agent;

    public ChatController(GeeconCoordinator agent) {
        this.agent = agent;
    }

    public record ChatRequest(String preferences) {}

    public record ChatResponse(String answer) {}

    public record ErrorResponse(String error, String kind) {}

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        Span.current().setAttribute("user.preferences", request.preferences());
        log.info("Chat request: {}", request.preferences());
        // Guardrails fire as the first step inside agent.coordinate(...) — see InputValidator.
        return new ChatResponse(agent.coordinate(request.preferences()));
    }

    @PostMapping(value = "/raw")
    public Map<String, String> raw(@RequestBody ChatRequest request) {
        Span.current().setAttribute("user.preferences", request.preferences());
        return Map.of("answer", agent.coordinate(request.preferences()));
    }

    /**
     * The InputValidator agent throws this when a guardrail blocks. langchain4j-agentic lets the
     * unchecked exception propagate up out of the sequence; we catch it here and translate to
     * HTTP 400 with the violation kind.
     */
    @ExceptionHandler(GuardrailViolationException.class)
    public ResponseEntity<ErrorResponse> handleGuardrailViolation(GuardrailViolationException ex) {
        log.warn("Guardrail blocked request: {} ({})", ex.getMessage(), ex.kind());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage(), ex.kind()));
    }
}

package com.comsenity.geecon.genai.coordinator.guardrail;

/**
 * Thrown when a Layer-1/Layer-2 input guardrail decides to block a request.
 * The {@link com.comsenity.geecon.genai.coordinator.web.ChatController}'s exception handler
 * maps this to HTTP 400 with the user-safe reason.
 */
public class GuardrailViolationException extends RuntimeException {

    private final String kind;

    public GuardrailViolationException(String kind, String message) {
        super(message);
        this.kind = kind;
    }

    public String kind() {
        return kind;
    }
}

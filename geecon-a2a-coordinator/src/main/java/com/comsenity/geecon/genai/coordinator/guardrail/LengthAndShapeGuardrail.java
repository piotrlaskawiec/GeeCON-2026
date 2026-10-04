package com.comsenity.geecon.genai.coordinator.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import org.springframework.stereotype.Component;

/**
 * Layer 1 — input shape sanity. Catches:
 *  - oversized payloads (default 2048 chars),
 *  - bidi-override Unicode (U+202A–U+202E, U+2066–U+2069) that can hide visible text,
 *  - non-printable control characters other than newline / tab,
 *  - private-use codepoints often used in steganographic prompt smuggling.
 *
 * <p>OWASP "LLM01: Prompt Injection" explicitly calls out non-visible / steganographic
 * injection variants — this guardrail addresses that surface.
 */
@Component
public class LengthAndShapeGuardrail implements InputGuardrail {

    public static final String KIND = "length_and_shape";
    private static final int MAX_CHARS = 2048;

    private final GuardrailMetrics metrics;
    private final GuardrailProperties props;

    public LengthAndShapeGuardrail(GuardrailMetrics metrics, GuardrailProperties props) {
        this.metrics = metrics;
        this.props = props;
    }

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        if (!props.length().enabled()) return success();
        String text = userMessage.singleText();
        if (text == null) return success();

        String reason = checkShape(text);
        if (reason == null) return success();

        String outcome = props.length().block() ? "blocked" : "flagged_only";
        metrics.record(KIND, "input", outcome);
        if (props.length().block()) {
            return fatal("Input rejected: " + reason);
        }
        return success();
    }

    private String checkShape(String text) {
        if (text.length() > MAX_CHARS) {
            return "exceeds " + MAX_CHARS + "-character limit";
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // Bidi-override and isolate codepoints
            if (c >= 0x202A && c <= 0x202E) return "contains bidi-override codepoint";
            if (c >= 0x2066 && c <= 0x2069) return "contains bidi-isolate codepoint";
            // Private-use area — frequently used to smuggle hidden tokens
            if (c >= 0xE000 && c <= 0xF8FF) return "contains private-use codepoint";
            // Control characters except \t (0x09), \n (0x0A), \r (0x0D)
            if (Character.isISOControl(c) && c != '\t' && c != '\n' && c != '\r') {
                return "contains control character U+" + String.format("%04X", (int) c);
            }
        }
        return null;
    }
}

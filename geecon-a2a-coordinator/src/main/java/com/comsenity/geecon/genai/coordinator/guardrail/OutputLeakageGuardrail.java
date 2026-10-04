package com.comsenity.geecon.genai.coordinator.guardrail;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Layer 3 — output guardrail wired on the {@code Summarizer}. Catches:
 *  - role-prefix lines in the model output (a "system:" / "assistant:" line at start of a line),
 *  - {@code javascript:} / {@code data:} / {@code vbscript:} URI scheme in markdown links/images,
 *  - very obvious system-prompt leak markers ("you are the GeeCON 2026 Coordinator"),
 *
 * <p>OWASP "LLM02: Insecure Output Handling" maps to this guardrail. On detection we return a
 * {@code failure(...)} which the langchain4j-agentic AiServices proxy turns into a sanitised
 * fallback answer, never the leaked content.
 */
@Component
public class OutputLeakageGuardrail implements OutputGuardrail {

    public static final String KIND = "output_leakage";

    private static final Pattern ROLE_PREFIX_LINE =
            Pattern.compile("(?im)^\\s*(system|assistant|user)\\s*:\\s*\\S+");
    private static final Pattern DANGEROUS_URI =
            Pattern.compile("(?i)\\]\\(\\s*(javascript|data|vbscript|file)\\s*:");
    private static final Pattern SYSTEM_PROMPT_LEAK =
            Pattern.compile("(?i)you\\s+are\\s+the\\s+GeeCON\\s+2026\\s+Coordinator");

    private final GuardrailMetrics metrics;
    private final GuardrailProperties props;

    public OutputLeakageGuardrail(GuardrailMetrics metrics, GuardrailProperties props) {
        this.metrics = metrics;
        this.props = props;
    }

    @Override
    public OutputGuardrailResult validate(AiMessage aiMessage) {
        if (!props.outputLeakage().enabled()) return success();
        String text = aiMessage.text();
        if (text == null || text.isBlank()) return success();

        if (ROLE_PREFIX_LINE.matcher(text).find()
                || DANGEROUS_URI.matcher(text).find()
                || SYSTEM_PROMPT_LEAK.matcher(text).find()) {
            String outcome = props.outputLeakage().block() ? "blocked" : "flagged_only";
            metrics.record(KIND, "output", outcome);
            if (props.outputLeakage().block()) {
                return failure("Output blocked: matches system-prompt leakage / unsafe URI / role-prefix pattern.");
            }
        }
        return success();
    }
}

package com.comsenity.geecon.genai.coordinator.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs Layer-1 + Layer-2 input guardrails imperatively from the {@code InputValidator} agent,
 * the first step of the langchain4j-agentic sequence. langchain4j-agentic-a2a's A2A proxies aren't
 * AiServices-backed, so the framework's annotation-driven guardrails don't fire on them.
 * Running the checks here also means a malicious input never burns A2A round-trips downstream.
 */
@Component
public class GuardrailRunner {

    private static final Logger log = LoggerFactory.getLogger(GuardrailRunner.class);

    private final List<InputGuardrail> guardrails;

    public GuardrailRunner(LengthAndShapeGuardrail length,
                           PromptInjectionGuardrail promptInjection,
                           LlmInjectionDetectorGuardrail llmDetector) {
        // Order matters: cheapest first (length/shape), then regex, then the costly LLM call.
        this.guardrails = List.of(length, promptInjection, llmDetector);
    }

    public void check(String preferences) {
        UserMessage msg = UserMessage.from(preferences);
        for (InputGuardrail guardrail : guardrails) {
            InputGuardrailResult result = guardrail.validate(msg);
            if (result.isFatal() || !result.isSuccess()) {
                String reason = result.failures().stream()
                        .findFirst()
                        .map(f -> ((InputGuardrailResult.Failure) f).message())
                        .orElse("input rejected");
                String kind = guardrail.getClass().getSimpleName();
                log.info("Guardrail {} blocked request: {}", kind, reason);
                throw new GuardrailViolationException(kind, reason);
            }
        }
    }
}

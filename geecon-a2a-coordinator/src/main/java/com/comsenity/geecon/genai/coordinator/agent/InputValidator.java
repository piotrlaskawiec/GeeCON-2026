package com.comsenity.geecon.genai.coordinator.agent;

import com.comsenity.geecon.genai.coordinator.guardrail.GuardrailRunner;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;
import org.springframework.stereotype.Component;

@Component
public class InputValidator {

    private final GuardrailRunner guardrailRunner;

    public InputValidator(GuardrailRunner guardrailRunner) {
        this.guardrailRunner = guardrailRunner;
    }

    @Agent(name = "validate",
            description = "Pre-flight input validation: rejects prompt-injection / unsafe-shape inputs before the chain runs.",
            outputKey = "preferences")
    public String validate(@V("preferences") String preferences) {
        guardrailRunner.check(preferences);
        return preferences;
    }
}

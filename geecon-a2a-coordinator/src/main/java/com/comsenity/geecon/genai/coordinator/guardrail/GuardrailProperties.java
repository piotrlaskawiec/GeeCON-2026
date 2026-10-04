package com.comsenity.geecon.genai.coordinator.guardrail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-guardrail toggles. Each guardrail can be:
 *  - disabled outright ({@code enabled=false}),
 *  - shadow mode: increments the metric, returns success (lets the request through),
 *  - block mode: increments the metric AND returns a fatal failure → request is rejected.
 *
 * <p>Block mode is the default for cheap/high-confidence layers (length, regex). The
 * LLM-detector layer ships in shadow mode by default so it can be observed before it gates
 * traffic — flip {@code geecon.guardrails.llm-detector.block: true} once happy with the data.
 */
@ConfigurationProperties(prefix = "geecon.guardrails")
public record GuardrailProperties(
        Toggle promptInjection,
        Toggle length,
        LlmDetector llmDetector,
        Toggle outputLeakage) {

    public GuardrailProperties {
        if (promptInjection == null) promptInjection = new Toggle(true, true);
        if (length == null) length = new Toggle(true, true);
        if (llmDetector == null) llmDetector = new LlmDetector(false, false, "gpt-5-nano", 64);
        if (outputLeakage == null) outputLeakage = new Toggle(true, true);
    }

    public record Toggle(boolean enabled, boolean block) {
        public Toggle { /* compact */ }
    }

    public record LlmDetector(boolean enabled, boolean block, String model, int minChars) {
        public LlmDetector { /* compact */ }
    }
}

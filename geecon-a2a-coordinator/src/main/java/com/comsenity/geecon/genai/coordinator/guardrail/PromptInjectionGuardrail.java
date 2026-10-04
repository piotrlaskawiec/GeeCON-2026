package com.comsenity.geecon.genai.coordinator.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Layer 1 — pattern-based prompt-injection / jailbreak detector.
 *
 * <p>Patterns are intentionally conservative: they only match phrases that are unambiguous
 * injection markers in the context of GeeCON-talk preferences. Bare keywords ("ignore",
 * "system") aren't enough on their own; the regex requires the surrounding instruction shape.
 * Inspired by the OWASP "LLM01: Prompt Injection" category.
 */
@Component
public class PromptInjectionGuardrail implements InputGuardrail {

    public static final String KIND = "prompt_injection";

    private static final List<Pattern> PATTERNS = List.of(
            // "ignore / disregard / forget [...] (previous|prior|above) (instructions|rules|prompts|guidelines)"
            Pattern.compile("(?i)\\b(ignore|disregard|forget|override)\\b[^\\n]{0,40}?\\b(?:previous|prior|above|all|the)\\b[^\\n]{0,30}?\\b(instructions?|prompts?|rules?|guidelines?)\\b"),
            // "reveal/show/tell me your (system|initial) prompt"
            Pattern.compile("(?i)\\b(reveal|show|tell|print|leak|dump)\\b[^\\n]{0,30}?\\b(system|initial|hidden|original)\\b[^\\n]{0,15}?\\b(prompt|instructions?|context|message)\\b"),
            // "what is your system prompt / instructions"
            Pattern.compile("(?i)\\bwhat\\s+(?:is|are)\\b[^\\n]{0,15}?\\byour\\b[^\\n]{0,15}?\\b(system\\s+prompt|instructions|guidelines|hidden\\s+rules)\\b"),
            // role-prefix injection: "system:" / "assistant:" / "user:" at start of line, surrounded by injection-y context
            Pattern.compile("(?im)^\\s*(system|assistant)\\s*:\\s*\\S+"),
            // "you are now (a|an) <persona>" — classic role-override
            Pattern.compile("(?i)\\byou\\s+are\\s+now\\b[^\\n]{0,15}?\\b(a|an|the)\\b\\s+\\w+"),
            // "pretend (to be|you are) <persona>"
            Pattern.compile("(?i)\\bpretend\\b[^\\n]{0,15}?\\b(to\\s+be|you\\s+are)\\b"),
            // "act as (a|an) <persona>" — injection variant; the "act as a developer" pattern
            Pattern.compile("(?i)\\bact\\s+as\\b\\s+(?:a|an|the)?\\s*(?:jailbroken|unfiltered|developer|root|admin|hacker|DAN|opposite|evil)\\b"),
            // Known DAN-style jailbreak handles
            Pattern.compile("(?i)\\b(DAN|do\\s+anything\\s+now|jailbreak\\s+mode|developer\\s+mode|god\\s+mode)\\b")
    );

    private final GuardrailMetrics metrics;
    private final GuardrailProperties props;

    public PromptInjectionGuardrail(GuardrailMetrics metrics, GuardrailProperties props) {
        this.metrics = metrics;
        this.props = props;
    }

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        if (!props.promptInjection().enabled()) return success();
        String text = userMessage.singleText();
        if (text == null || text.isBlank()) return success();

        for (Pattern p : PATTERNS) {
            if (p.matcher(text).find()) {
                String outcome = props.promptInjection().block() ? "blocked" : "flagged_only";
                metrics.record(KIND, "input", outcome);
                if (props.promptInjection().block()) {
                    return fatal("Input flagged as a possible prompt-injection attempt.");
                }
                return success();
            }
        }
        return success();
    }
}

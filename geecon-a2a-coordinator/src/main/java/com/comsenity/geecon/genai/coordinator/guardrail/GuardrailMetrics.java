package com.comsenity.geecon.genai.coordinator.guardrail;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import org.springframework.stereotype.Component;

/**
 * Single Micrometer counter — {@code guardrail.violations.total} — incremented every time a
 * guardrail flags or blocks input/output. Tags follow the same shape as the gen_ai metrics so
 * the dashboard can drill consistently.
 *
 * <p>Also stamps the active OpenTelemetry span with {@code guardrail.violation} so violations
 * are searchable in Tempo as filterable span attributes.
 */
@Component
public class GuardrailMetrics {

    public static final String METRIC_NAME = "guardrail.violations.total";

    private final MeterRegistry meterRegistry;

    public GuardrailMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(String kind, String phase, String outcome) {
        Counter.builder(METRIC_NAME)
                .description("Number of guardrail violations detected on the coordinator entry/exit")
                .tag("guardrail.kind", kind)
                .tag("guardrail.phase", phase)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();

        Span span = Span.current();
        if (span != null && span.getSpanContext().isValid()) {
            span.setAttribute("guardrail.violation", kind);
            span.setAttribute("guardrail.outcome", outcome);
        }
    }
}

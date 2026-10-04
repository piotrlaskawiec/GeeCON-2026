package com.comsenity.geecon.genai.coordinator.observability;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Forces explicit-bucket histograms on every {@code gen_ai.*} meter.
 *
 * <p>Without this, the {@code DistributionSummary} created by langchain4j-observation's
 * {@code ObservationChatModelListener} for {@code gen_ai.client.token.usage} ships through the
 * Micrometer OTLP registry as a zero-bucket exponential histogram. The LGTM OTel collector
 * silently drops those, so even though {@code /actuator/prometheus} reports the {@code _sum} /
 * {@code _count} series, nothing reaches Grafana.
 */
@Configuration
public class GenAiMetricsConfig {

    @Bean
    public MeterFilter genAiHistogramFilter() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                String name = id.getName();
                if (name.startsWith("gen_ai.") || name.startsWith("gen.ai.")) {
                    return DistributionStatisticConfig.builder()
                            .percentilesHistogram(true)
                            .serviceLevelObjectives(
                                    1, 8, 64, 512, 1_024, 2_048, 4_096, 8_192, 16_384, 32_768, 65_536)
                            .build()
                            .merge(config);
                }
                return config;
            }
        };
    }
}

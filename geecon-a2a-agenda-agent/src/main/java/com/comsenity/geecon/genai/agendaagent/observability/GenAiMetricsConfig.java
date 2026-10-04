package com.comsenity.geecon.genai.agendaagent.observability;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Forces explicit-bucket histograms on every {@code gen_ai.*} meter. Spring AI 2.x already
 * configures buckets for its own observations, but this catch-all filter guarantees consistent
 * OTLP histogram shape across all three agents in the chain.
 */
@Configuration
public class GenAiMetricsConfig {

    @Bean
    public MeterFilter genAiHistogramFilter() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                String name = id.getName();
                if (name.startsWith("gen_ai.") || name.startsWith("gen.ai.") || name.startsWith("spring.ai.")) {
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

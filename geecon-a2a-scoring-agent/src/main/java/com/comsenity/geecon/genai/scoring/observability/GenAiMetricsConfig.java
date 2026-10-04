package com.comsenity.geecon.genai.scoring.observability;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Forces explicit-bucket histograms on every {@code gen_ai.*} meter so Spring AI 1.1.x's
 * {@code DistributionSummary}s (used by Embabel under the hood) export through the OTLP
 * registry as ExplicitBucketHistograms — the LGTM OTel collector silently drops zero-bucket
 * exponential histograms.
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

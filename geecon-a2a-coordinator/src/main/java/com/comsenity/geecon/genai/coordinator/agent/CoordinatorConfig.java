package com.comsenity.geecon.genai.coordinator.agent;

import com.comsenity.geecon.genai.coordinator.guardrail.OutputLeakageGuardrail;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.observation.listener.ObservationChatModelListener;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.List;

/**
 * Wires the langchain4j-agentic workflow:
 *   1. Two A2A client proxies for the remote agents (built via {@code AgenticServices.a2aBuilder}).
 *   2. A local OpenAI summarizer (built via {@code AgenticServices.agentBuilder}) — Layer-3
 *      output guardrail attaches here.
 *   3. A small, separate {@code guardrailChatModel} bean used by the Layer-2 LLM injection
 *      detector — distinct from {@link #chatModel} so the detector cannot be subverted by
 *      reusing the orchestrator's system prompt.
 *   4. A sequential composition that produces the {@link GeeconCoordinator} entry point.
 */
@Configuration
public class CoordinatorConfig {

    @Bean
    public GeeconCoordinator coordinator(InputValidator inputValidator,
                                         AgendaService agendaService,
                                         ScoringService scoringService,
                                         Summarizer summarizer) {
        // InputValidator is FIRST so Layer-1 + Layer-2 input guardrails fire before any
        // A2A round-trip or LLM call. It re-emits `preferences` unchanged so downstream
        // sub-agents see the same agentic-scope value they used to.
        return AgenticServices.sequenceBuilder(GeeconCoordinator.class)
                .name("geecon-coordinator")
                .description("Sequence: validate → agenda → scoring → summarize")
                .subAgents(inputValidator, agendaService, scoringService, summarizer)
                .outputKey("summary")
                .build();
    }

    @Bean
    public AgendaService agendaService(@Value("${geecon.a2a.agenda-url}") String url) {
        return AgenticServices.a2aBuilder(url, AgendaService.class)
                .inputKeys("preferences")
                .outputKey("talks")
                .build();
    }

    @Bean
    public ScoringService scoringService(@Value("${geecon.a2a.scoring-url}") String url) {
        return AgenticServices.a2aBuilder(url, ScoringService.class)
                .inputKeys("talks")
                .outputKey("scoredTalks")
                .build();
    }

    @Bean
    public Summarizer summarizer(ChatModel chatModel, OutputLeakageGuardrail outputLeakageGuardrail) {
        return AgenticServices.agentBuilder(Summarizer.class)
                .chatModel(chatModel)
                .outputGuardrails(outputLeakageGuardrail)
                .outputKey("summary")
                .build();
    }

    @Bean
    public ChatModel chatModel(@Value("${openai.api-key}") String apiKey,
                               @Value("${openai.model:gpt-5}") String model,
                               ObservationRegistry observationRegistry,
                               MeterRegistry meterRegistry) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .listeners(List.of(new ObservationChatModelListener(observationRegistry, meterRegistry)))
                .build();
    }

    @Bean(name = "guardrailChatModel")
    public ChatModel guardrailChatModel(@Value("${openai.api-key}") String apiKey,
                                        @Value("${geecon.guardrails.llm-detector.model:gpt-5-nano}") String model,
                                        ObservationRegistry observationRegistry,
                                        MeterRegistry meterRegistry) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .listeners(List.of(new ObservationChatModelListener(observationRegistry, meterRegistry)))
                .build();
    }
}

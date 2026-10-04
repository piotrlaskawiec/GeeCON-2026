package com.comsenity.geecon.genai.agendaagent.a2a;

import com.comsenity.geecon.genai.agenda.ConferenceTalk;
import com.comsenity.geecon.genai.agendaagent.agent.AgendaAgent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.TextPart;
import io.a2a.spec.TransportProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Beans required by {@code spring-ai-a2a-server-autoconfigure}: the public {@link AgentCard}
 * advertising this agent and the {@link AgentExecutor} that handles {@code message/send} calls.
 *
 * <p>The response artifact is delivered as a {@link TextPart} carrying the JSON-encoded talks
 * payload — langchain4j-agentic-a2a's {@code A2AClientAgentInvoker} only reads {@code TextPart}
 * data from response artifacts; a {@code DataPart} would surface as an empty value in the
 * coordinator's agentic scope and propagate downstream as an empty user message.
 */
@Configuration
public class A2AConfig {

    private static final Logger log = LoggerFactory.getLogger(A2AConfig.class);

    @Bean
    public AgentCard agentCard(@Value("${geecon.a2a.public-url:http://localhost:8081}") String publicUrl) {
        return new AgentCard.Builder()
                .name("GeeCON Agenda Agent")
                .description("Curates GeeCON 2026 talks against user-supplied interests using the live agenda from the GeeCON MCP server.")
                .version("1.0.0")
                .url(publicUrl)
                .preferredTransport(TransportProtocol.JSONRPC.asString())
                .additionalInterfaces(List.of(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), publicUrl)))
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(false)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text/plain"))
                .defaultOutputModes(List.of("text/plain"))
                .skills(List.of(new AgentSkill.Builder()
                        .id("recommend-sessions")
                        .name("Recommend GeeCON Sessions")
                        .description("Returns a JSON array of GeeCON 2026 ConferenceTalk objects matching the user's interests, encoded as a single TextPart.")
                        .tags(List.of("agenda", "conference", "geecon", "recommendations"))
                        .inputModes(List.of("text/plain"))
                        .outputModes(List.of("text/plain"))
                        .examples(List.of("Recommend talks about platform engineering, Java performance, and observability"))
                        .build()))
                .build();
    }

    @Bean
    public AgentExecutor agendaAgentExecutor(AgendaAgent agendaAgent) {
        return new AgendaAgentExecutor(agendaAgent);
    }

    private static final class AgendaAgentExecutor implements AgentExecutor {

        // Local Jackson 2 mapper — Spring Boot 4 / Spring AI 2 standardized on Jackson 3
        // (tools.jackson) internally, so the classic com.fasterxml.jackson ObjectMapper is
        // no longer auto-registered as a Spring bean.
        private final ObjectMapper objectMapper = new ObjectMapper();
        private final AgendaAgent agendaAgent;

        AgendaAgentExecutor(AgendaAgent agendaAgent) {
            this.agendaAgent = agendaAgent;
        }

        @Override
        public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
            TaskUpdater updater = new TaskUpdater(context, eventQueue);
            if (context.getTask() == null) {
                updater.submit();
            }
            updater.startWork();

            String preferences = extractText(context.getMessage());
            log.info("Recommending talks for preferences: {}", preferences);

            List<ConferenceTalk> recommended = agendaAgent.recommend(preferences);

            // TextPart (not DataPart) so langchain4j-agentic-a2a's A2AClientAgentInvoker can read
            // the JSON back; the coordinator's agentic scope receives this string under the
            // recommend()'s outputKey and forwards it to the scoring agent verbatim.
            String json;
            try {
                json = objectMapper.writeValueAsString(recommended);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to serialize recommended talks", ex);
            }
            updater.addArtifact(List.of(new TextPart(json)));
            updater.complete();
        }

        @Override
        public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
            new TaskUpdater(context, eventQueue).cancel();
        }

        private String extractText(Message message) {
            return message.getParts().stream()
                    .filter(p -> p instanceof TextPart)
                    .map(p -> ((TextPart) p).getText())
                    .collect(Collectors.joining());
        }
    }
}

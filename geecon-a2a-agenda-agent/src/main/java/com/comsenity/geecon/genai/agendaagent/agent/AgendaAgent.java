package com.comsenity.geecon.genai.agendaagent.agent;

import com.comsenity.geecon.genai.agenda.ConferenceTalk;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgendaAgent {

    private static final String SYSTEM_PROMPT = """
            You are the GeeCON 2026 Agenda Curator. You receive:
              1. The full GeeCON 2026 agenda (via the geecon-agenda MCP tool).
              2. A free-text description of a user's interests.

            Your job: pick the talks that best match those interests. Be generous (include borderline-related
            talks) but not exhaustive — aim for 8-15 candidates. Do not invent talks. Return ONLY data that
            came from the geecon-agenda tool.
            """;

    private final ChatClient chatClient;
    private final Tracer tracer;

    public AgendaAgent(ChatClient.Builder chatClientBuilder,
                       SyncMcpToolCallbackProvider toolCallbackProvider,
                       Tracer tracer
    ) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultToolCallbacks(toolCallbackProvider.getToolCallbacks())
                .build();
        this.tracer = tracer;
    }

    public List<ConferenceTalk> recommend(String preferences) {
        Span span = tracer.spanBuilder("agenda.recommend")
                .setAttribute("user.preferences", preferences)
                .startSpan();
        try (var ignored = span.makeCurrent()) {
            return chatClient
                    .prompt()
                    .user("""
                            User preferences:
                            ----
                            %s
                            ----
                            Use the geecon-agenda tool to list all talks, then pick the most relevant ones.
                            Return them as a JSON array preserving the original talk structure (title, speaker,
                            date, timeSlot, room, category).
                            """.formatted(preferences))
                    .call()
                    .entity(new org.springframework.core.ParameterizedTypeReference<List<ConferenceTalk>>() {});
        } finally {
            span.end();
        }
    }
}

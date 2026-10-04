package com.comsenity.geecon.genai.scoring.agent;

import com.embabel.agent.domain.library.HasContent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Goal type returned by {@link SessionScoringAgent}. Implements {@link HasContent} so Embabel's
 * {@code AutonomyA2ARequestHandler.createResultArtifact} adds a {@code TextPart} carrying
 * {@link #getContent()} (the JSON-encoded scores list) to the A2A response artifact — only then
 * does langchain4j-agentic-a2a's {@code A2AClientAgentInvoker} read the value back, since it
 * scans response artifact parts for {@code TextPart} only and ignores {@code DataPart}.
 */
public record ScoredTalks(List<TalkScore> scores) implements HasContent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String getContent() {
        try {
            return MAPPER.writeValueAsString(scores);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to serialize ScoredTalks", ex);
        }
    }
}

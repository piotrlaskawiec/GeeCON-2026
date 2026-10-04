package com.comsenity.geecon.genai.coordinator.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

public interface ScoringService {

    @Agent(name = "scoring",
            description = "Scores GeeCON 2026 talks (1-5) on topic + speaker dimensions",
            outputKey = "scoredTalks")
    String score(@V("talks") String talks);
}

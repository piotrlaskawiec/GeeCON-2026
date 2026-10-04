package com.comsenity.geecon.genai.coordinator.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

public interface AgendaService {

    @Agent(name = "agenda",
            description = "Recommends GeeCON 2026 talks matching the supplied user preferences",
            outputKey = "talks")
    String recommend(@V("preferences") String preferences);
}

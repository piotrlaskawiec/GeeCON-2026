package com.comsenity.geecon.genai.coordinator.agent;

import dev.langchain4j.service.V;

public interface GeeconCoordinator {

    String coordinate(@V("preferences") String preferences);
}

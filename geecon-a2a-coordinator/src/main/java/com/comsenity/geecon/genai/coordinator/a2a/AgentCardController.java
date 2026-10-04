package com.comsenity.geecon.genai.coordinator.a2a;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class AgentCardController {

    @Value("${geecon.a2a.public-url:http://localhost:8080}")
    private String publicUrl;

    @GetMapping("/.well-known/agent.json")
    public Map<String, Object> agentCard() {
        return Map.of(
                "protocolVersion", "0.3.0",
                "name", "GeeCON Coordinator",
                "description", "Top-level coordinator that delegates to the GeeCON Agenda and Scoring agents to recommend top-graded GeeCON 2026 talks.",
                "version", "1.0.0",
                "url", publicUrl + "/chat",
                "preferredTransport", "JSONRPC",
                "capabilities", Map.of(
                        "streaming", false,
                        "pushNotifications", false),
                "defaultInputModes", List.of("text/plain"),
                "defaultOutputModes", List.of("text/plain"),
                "skills", List.of(Map.of(
                        "id", "recommend-top-talks",
                        "name", "Recommend top-graded GeeCON talks",
                        "description", "Given user interests, returns the top-scored matching GeeCON 2026 talks by orchestrating the agenda and scoring agents over A2A.",
                        "tags", List.of("geecon", "recommendations", "coordinator"),
                        "examples", List.of("Recommend top GeeCON 2026 talks about platform engineering, Java performance, and observability"))));
    }
}

package com.comsenity.geecon.genai.mcp.service;

import com.comsenity.geecon.genai.agenda.ConferenceTalk;
import com.comsenity.geecon.genai.agenda.GeeconAgendaExtractor;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.mcp.annotation.context.MetaProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class GeeconScheduleApp {

    @Value("classpath:/app/schedule-app.html")
    private Resource scheduleAppResource;

    @McpResource(
            name = "GeeCON Schedule App",
            uri = "ui://geecon/schedule-app.html",
            mimeType = "text/html;profile=mcp-app",
            metaProvider = ScheduleCspMetaProvider.class)
    @Cacheable("talks-resource")
    public String getScheduleApp() throws IOException {
        return scheduleAppResource.getContentAsString(StandardCharsets.UTF_8);
    }

    @McpResource(
            name = "GeeCON Sessions Data",
            uri = "data://geecon/sessions",
            mimeType = "application/json")
    public String getAllSessions() throws JacksonException {
        return objectMapper.writeValueAsString(new GeeconAgendaExtractor().fetchConferenceTalks());
    }

    @McpTool(
            title = "GeeCON Planner",
            name = "geecon-planner",
            description = """
                    Opens an interactive visual schedule of GeeCON 2026 sessions.
                    Users can select their interests from the predefined categories to highlight matching sessions in green.
                    Sessions can be selected and added back to the conversation as a personalised agenda.
                    Planner should be only used if a user wants to manually select the sessions.
                    Planner can't automatically prefilter available sessions - use geecon-agenda tool for more advanced reasoning.
                    When reopening to modify a previous selection, pass the previouslySelectedSessions value
                    from the user's plan context (a JSON array of session keys) to restore their selection.
                    """,
            metaProvider = ScheduleToolMetaProvider.class)
    public String geeConPlanner(@McpToolParam(required = false, description = "JSON array of previously selected session keys") String previouslySelectedSessions) {
        return "Opening the interactive GeeCON 2026 schedule. "
                + "Filter by category to highlight relevant sessions, "
                + "then select the ones you want to attend and click 'Add to my plan'.";
    }

    @McpTool(title = "GeeCON Agenda", name = "geecon-agenda", description = "Get GeeCON talks (including title, speaker name, date, timeslot and the conference room)")
    @Cacheable("talks")
    public List<ConferenceTalk> fetchConferenceTalks() {
        return new GeeconAgendaExtractor().fetchConferenceTalks();
    }

    public static final class ScheduleCspMetaProvider implements MetaProvider {
        @Override
        public Map<String, Object> getMeta() {
            return Map.of("ui",
                    Map.of("csp",
                            Map.of("resourceDomains",
                                    List.of("https://unpkg.com"))));
        }
    }

    public static final class ScheduleToolMetaProvider implements MetaProvider {
        @Override
        public Map<String, Object> getMeta() {
            return Map.of("ui",
                    Map.of("resourceUri", "ui://geecon/schedule-app.html"));
        }
    }

    //region NOT INTERESTING
    private ObjectMapper objectMapper = new  ObjectMapper();
    //endregion
}

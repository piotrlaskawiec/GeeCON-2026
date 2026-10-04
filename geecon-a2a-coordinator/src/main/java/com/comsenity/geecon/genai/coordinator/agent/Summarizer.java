package com.comsenity.geecon.genai.coordinator.agent;

import com.comsenity.geecon.genai.coordinator.guardrail.OutputLeakageGuardrail;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.guardrail.OutputGuardrails;

/**
 * Local langchain4j-agentic step (OpenAI gpt-5.2). Closes the workflow by formatting the scored
 * talks into a readable answer for the user.
 */
public interface Summarizer {

    @Agent(name = "summarize",
            description = "Picks the top-scored GeeCON talks and formats a friendly markdown summary for the user.",
            outputKey = "summary")
    @OutputGuardrails(OutputLeakageGuardrail.class)
    @UserMessage("""
            You are the GeeCON 2026 Coordinator. The user asked for talk recommendations.

            Their stated preferences:
            ----
            {{preferences}}
            ----

            The remote scoring agent returned this JSON list of scored talks (each entry has
            talk{title,speaker,date,timeSlot,room,category}, topicScore, speakerScore, totalScore,
            justification):
            ----
            {{scoredTalks}}
            ----

            Produce a short markdown answer:
              1. A 1-2 sentence intro acknowledging the user's interests.
              2. The top 5 talks ordered by totalScore descending. For each, render:
                 - **Title** by Speaker — Day, Time, Room
                 - Score: x.xx — one-sentence justification
              3. Nothing else.
            """)
    String summarize(@V("preferences") String preferences, @V("scoredTalks") String scoredTalks);
}

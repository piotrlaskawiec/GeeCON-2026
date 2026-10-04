package com.comsenity.geecon.genai.scoring.agent;

import com.comsenity.geecon.genai.agenda.ConferenceTalk;

import java.util.List;

/**
 * Parsed scoring request: the batch of {@link ConferenceTalk} to score, extracted from the raw
 * JSON in the inbound {@code UserInput}.
 *
 * <p>Embabel's planner uses the type system to discover the action graph. By making this its
 * own type (instead of mixing parsing with scoring inside one method) the planner clearly sees:
 * <pre>
 *   UserInput → ScoringRequest → TopicScores  ─┐
 *                              → SpeakerScores ─┴→ ScoredTalks
 * </pre>
 */
public record ScoringRequest(List<ConferenceTalk> talks) {
}

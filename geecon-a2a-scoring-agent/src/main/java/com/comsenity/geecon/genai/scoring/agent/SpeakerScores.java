package com.comsenity.geecon.genai.scoring.agent;

import java.util.List;

/**
 * Embabel goal-graph node: the speaker-dimension scores for every talk in a {@link ScoringRequest}.
 */
public record SpeakerScores(List<SpeakerScored> entries) {
}

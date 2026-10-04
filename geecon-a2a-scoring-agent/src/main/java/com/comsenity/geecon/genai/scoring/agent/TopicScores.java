package com.comsenity.geecon.genai.scoring.agent;

import java.util.List;

/**
 * Embabel goal-graph node: the topic-dimension scores for every talk in a {@link ScoringRequest}.
 * Distinct type so the planner can run topic and speaker scoring as independent steps and
 * combine them in the final aggregation action.
 */
public record TopicScores(List<TopicScored> entries) {
}

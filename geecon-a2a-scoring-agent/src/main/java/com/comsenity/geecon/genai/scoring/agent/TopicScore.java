package com.comsenity.geecon.genai.scoring.agent;

public record TopicScore(
        int relevance,
        int novelty,
        int innovativeness,
        int repeatability,
        String reasoning) {

    public double average() {
        return (relevance + novelty + innovativeness + repeatability) / 4.0;
    }
}

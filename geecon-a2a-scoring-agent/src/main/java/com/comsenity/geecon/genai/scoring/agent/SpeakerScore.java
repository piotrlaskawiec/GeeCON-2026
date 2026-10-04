package com.comsenity.geecon.genai.scoring.agent;

public record SpeakerScore(
        int trackRecord,
        int experience,
        int subjectMatterExpertise,
        String reasoning) {

    public double average() {
        return (trackRecord + experience + subjectMatterExpertise) / 3.0;
    }
}

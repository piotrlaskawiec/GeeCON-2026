package com.comsenity.geecon.genai.scoring.agent;

import com.comsenity.geecon.genai.agenda.ConferenceTalk;

public record TalkScore(
        ConferenceTalk talk,
        TopicScore topicScore,
        SpeakerScore speakerScore,
        double totalScore,
        String justification) {
}

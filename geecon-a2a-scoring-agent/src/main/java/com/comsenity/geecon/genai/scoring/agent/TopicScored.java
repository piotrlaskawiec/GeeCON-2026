package com.comsenity.geecon.genai.scoring.agent;

import com.comsenity.geecon.genai.agenda.ConferenceTalk;

/** Pair of a single {@link ConferenceTalk} and its computed {@link TopicScore}. */
public record TopicScored(ConferenceTalk talk, TopicScore score) {
}

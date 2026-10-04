package com.comsenity.geecon.genai.scoring.agent;

import com.comsenity.geecon.genai.agenda.ConferenceTalk;
import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.domain.io.UserInput;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Agent(description = "Scores GeeCON 2026 conference talks on topic and speaker dimensions (1-5)")
public class SessionScoringAgent {

    @Action
    public ScoringRequest parseRequest(UserInput input) {
        return toScoringRequest(input);
    }

    @Action
    public TopicScores scoreTopics(ScoringRequest request, OperationContext context) {
        log.info("Scoring topics for {} talks (parallelMap, concurrency={})", request.talks().size(), CONCURRENCY);

        List<TopicScored> entries = context.parallelMap(request.talks(), CONCURRENCY, talk -> {
            String prompt = """
                    Score the following conference talk's TOPIC on a 1-5 scale across these dimensions:
                      - relevance: how relevant is the topic to a senior JVM engineering audience in 2026
                      - novelty: how fresh is the angle compared to typical conference content
                      - innovativeness: does it surface a genuinely new technique, library, or insight
                      - repeatability: 5 = appears to be original talk; 1 = recycled from many other conferences

                    Title: %s
                    Speaker: %s
                    Category: %s

                    Return only the requested fields and a one-sentence reasoning.
                    """.formatted(talk.title(), talk.speaker(), talk.category());
            TopicScore score = context.ai().withAutoLlm().createObject(prompt, TopicScore.class);
            return new TopicScored(talk, score);
        });
        return new TopicScores(entries);
    }

    @Action
    public SpeakerScores scoreSpeakers(ScoringRequest request, OperationContext context) {
        log.info("Scoring speakers for {} talks (parallelMap, concurrency={})", request.talks().size(), CONCURRENCY);
        List<SpeakerScored> entries = context.parallelMap(request.talks(), CONCURRENCY, talk -> {
            String prompt = """
                    Score the speaker on a 1-5 scale across these dimensions, based on what is publicly known
                    about them. If you do not have strong evidence, score 3 and say so in reasoning.
                      - trackRecord: history of well-received talks
                      - experience: years and depth in the field
                      - subjectMatterExpertise: depth in the specific topic of THIS talk

                    Talk title: %s
                    Speaker: %s

                    Return only the requested fields and a one-sentence reasoning.
                    """.formatted(talk.title(), talk.speaker());
            SpeakerScore score = context.ai().withAutoLlm().createObject(prompt, SpeakerScore.class);
            return new SpeakerScored(talk, score);
        });
        return new SpeakerScores(entries);
    }

    @AchievesGoal(description = "Aggregated 1-5 score for each supplied GeeCON talk")
    @Action
    public ScoredTalks aggregate(ScoringRequest request, TopicScores topics, SpeakerScores speakers) {
        log.info("Aggregating scores for {} talks", request.talks().size());

        List<TalkScore> scores = performScoring(request, topics, speakers);

        return new ScoredTalks(scores);
    }

    //region NOT IMPORTANT
    private static final Logger log = LoggerFactory.getLogger(SessionScoringAgent.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Max simultaneous LLM calls per scoring action. Chosen to balance demo wallclock vs. provider rate limits. */
    private static final int CONCURRENCY = 8;

    private List<TalkScore> performScoring(ScoringRequest request, TopicScores topics, SpeakerScores speakers) {
        List<TalkScore> scores = new ArrayList<>(request.talks().size());
        for (ConferenceTalk talk : request.talks()) {
            TopicScore topic = topics.entries().stream()
                    .filter(e -> e.talk().equals(talk))
                    .map(TopicScored::score)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No topic score for " + talk.title()));
            SpeakerScore speaker = speakers.entries().stream()
                    .filter(e -> e.talk().equals(talk))
                    .map(SpeakerScored::score)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No speaker score for " + talk.title()));

            double total = (topic.average() + speaker.average()) / 2.0;
            String justification = "Topic %.2f (relevance=%d, novelty=%d, innovation=%d, repeatability=%d). Speaker %.2f (track=%d, experience=%d, smex=%d)."
                    .formatted(
                            topic.average(),
                            topic.relevance(), topic.novelty(), topic.innovativeness(), topic.repeatability(),
                            speaker.average(),
                            speaker.trackRecord(), speaker.experience(), speaker.subjectMatterExpertise());
            scores.add(new TalkScore(talk, topic, speaker, Math.round(total * 100.0) / 100.0, justification));
        }

        return scores;
    }

    private ScoringRequest toScoringRequest(UserInput input) {
        try {
            String json = input.getContent().trim();
            if (json.startsWith("{")) {
                var node = objectMapper.readTree(json);
                if (node.has("talks")) {
                    return new ScoringRequest(objectMapper.convertValue(
                            node.get("talks"),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, ConferenceTalk.class)));
                }
            }
            List<ConferenceTalk> talks = objectMapper.readValue(json, new TypeReference<>() {});
            log.info("Parsed scoring request with {} talks", talks.size());
            return new ScoringRequest(talks);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Scoring agent expects a JSON array of ConferenceTalk in the user message; got: " + input.getContent(), ex);
        }
    }
    //endregion
}

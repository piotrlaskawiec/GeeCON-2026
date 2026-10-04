package com.comsenity.geecon.genai.agenda;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GeeconAgendaExtractor {

    public List<ConferenceTalk> fetchConferenceTalks() {
        return Stream.of(
                getTalksFrom("https://remadag.github.io/geecon-schedule/schedule-day-1/", LocalDate.of(2026, 5, 14)),
                getTalksFrom("https://remadag.github.io/geecon-schedule/schedule-day-2/", LocalDate.of(2026, 5, 15))
        ).flatMap(Collection::stream).collect(Collectors.toList());
    }

    private List<ConferenceTalk> getTalksFrom(String url, LocalDate date) {
        List<ConferenceTalk> conferenceTalks = new ArrayList<>();

        try {
            Elements timeslots = Jsoup.connect(url).get().select(".timeslot:not(.track-header)");
            timeslots.forEach(timeslot -> {
                conferenceTalks.addAll(extractSlot(timeslot, date));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return conferenceTalks;
    }

    private List<ConferenceTalk> extractSlot(Element timeSlot, LocalDate date) {
        return timeSlot.select(".slot").stream().map(slot -> new ConferenceTalk(
                slot.selectFirst(".slot-title").ownText(),
                slot.select(".speaker-name").stream().map(speakerName -> speakerName.ownText()).collect(Collectors.joining(", ")),
                date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)),
                extractTimeSlot(timeSlot),
                slot.attr("data-slot-detail"),
                slot.selectFirst(".slot-language") != null ? slot.selectFirst(".slot-language").ownText() : null)).toList();
    }

    private String extractTimeSlot(Element timeSlot) {
        String startTime = extractTime(timeSlot.selectFirst(".start-time"));
        String endTime = extractTime(timeSlot.selectFirst(".end-time"));

        return startTime + "-" + endTime;
    }

    private String extractTime(Element timeHolder) {
        return unifyHours(timeHolder.ownText()) + ":" + timeHolder.getElementsByTag("span").first().ownText();
    }

    private String unifyHours(String hours) {
        String trimmed = hours.trim();
        return trimmed.length() == 2 ? trimmed : "0" + trimmed;
    }
}

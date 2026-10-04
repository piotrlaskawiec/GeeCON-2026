package com.comsenity.geecon.genai.agenda;

public record ConferenceTalk(
        String title,
        String speaker,
        String date,
        String timeSlot,
        String room,
        String category) {
}

package service;

import model.Event;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class ICalService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    public void exportEvents(List<Event> events, File file) throws IOException {
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("BEGIN:VCALENDAR\r\n");
            fw.write("VERSION:2.0\r\n");
            fw.write("PRODID:-//EVENTO//EventoApp//EN\r\n");
            fw.write("CALSCALE:GREGORIAN\r\n");
            fw.write("METHOD:PUBLISH\r\n");
            for (Event e : events) {
                fw.write("BEGIN:VEVENT\r\n");
                fw.write("UID:" + UUID.randomUUID() + "@evento\r\n");
                if (e.getDateHeure() != null) {
                    fw.write("DTSTART:" + e.getDateHeure().format(FMT) + "\r\n");
                    fw.write("DTEND:" + e.getDateHeure().plusHours(2).format(FMT) + "\r\n");
                }
                fw.write("SUMMARY:" + esc(e.getTitre()) + "\r\n");
                if (e.getDescription() != null && !e.getDescription().isBlank())
                    fw.write("DESCRIPTION:" + esc(e.getDescription()) + "\r\n");
                if (e.getVenue() != null && !e.getVenue().isBlank())
                    fw.write("LOCATION:" + esc(e.getVenue()) + "\r\n");
                fw.write("STATUS:" + (e.getStatut() != null ? e.getStatut().toUpperCase() : "CONFIRMED") + "\r\n");
                fw.write("END:VEVENT\r\n");
            }
            fw.write("END:VCALENDAR\r\n");
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}

package service;

import model.Event;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

public class EventStatsService {

    private final EventService eventService = new EventService();

    public long totalCount() throws SQLException {
        return eventService.recuperer().size();
    }

    public Map<String, Long> countByStatus() throws SQLException {
        return eventService.recuperer().stream()
                .collect(Collectors.groupingBy(
                        e -> e.getStatut() != null ? e.getStatut() : "unknown",
                        Collectors.counting()
                ));
    }

    public long countByStatut(String statut) throws SQLException {
        return eventService.recuperer().stream()
                .filter(e -> statut.equalsIgnoreCase(e.getStatut()))
                .count();
    }

    public double averageCapacity() throws SQLException {
        return eventService.recuperer().stream()
                .mapToInt(Event::getCapacite)
                .average()
                .orElse(0);
    }

    public int totalCapacitySum() throws SQLException {
        return eventService.recuperer().stream()
                .mapToInt(Event::getCapacite)
                .sum();
    }

    public long upcomingCount() throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        return eventService.recuperer().stream()
                .filter(e -> e.getDateHeure() != null && e.getDateHeure().isAfter(now))
                .filter(e -> !"cancelled".equalsIgnoreCase(e.getStatut()))
                .count();
    }

    public Map<String, Long> topGenres(int limit) throws SQLException {
        return eventService.recuperer().stream()
                .filter(e -> e.getGenre() != null && !e.getGenre().isBlank())
                .collect(Collectors.groupingBy(Event::getGenre, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    public Map<Month, Long> eventsPerMonth() throws SQLException {
        return eventService.recuperer().stream()
                .filter(e -> e.getDateHeure() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getDateHeure().getMonth(),
                        Collectors.counting()
                ));
    }

    public Optional<Event> highestCapacityEvent() throws SQLException {
        return eventService.recuperer().stream()
                .max(Comparator.comparingInt(Event::getCapacite));
    }

    public List<Event> getUpcoming(int limit) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        return eventService.recuperer().stream()
                .filter(e -> e.getDateHeure() != null && e.getDateHeure().isAfter(now))
                .filter(e -> !"cancelled".equalsIgnoreCase(e.getStatut()))
                .sorted(Comparator.comparing(Event::getDateHeure))
                .limit(limit)
                .collect(Collectors.toList());
    }
}

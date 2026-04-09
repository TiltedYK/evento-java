package test;

import model.Event;
import model.Reservation;
import service.EventService;
import service.ReservationService;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class Main {

    public static void main(String[] args) {

        // ==========================================
        // 1) EVENT — CRUD + SEARCH
        // ==========================================
        EventService es = new EventService();

        // --- AJOUTER ---
        try {
            Event ev = new Event(
                    "Concert Rock",
                    LocalDateTime.of(2026, 7, 15, 20, 0),
                    200,
                    "Un super concert de rock",
                    "active",
                    "Salle Carthage"
            );
            ev.setLocation("Tunis");
            ev.setGenre("Rock");
            es.ajouter(ev);
            System.out.println("Event ajouté !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // --- RECUPERER ---
        try {
            System.out.println("=== Tous les événements ===");
            es.recuperer().forEach(System.out::println);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // --- MODIFIER (change id selon ta DB) ---
        try {
            Event toUpdate = new Event();
            toUpdate.setId(1);  // <-- adapte cet ID
            toUpdate.setTitre("Concert Rock UPDATED");
            toUpdate.setDateHeure(LocalDateTime.of(2026, 8, 20, 21, 0));
            toUpdate.setCapacite(300);
            toUpdate.setDescription("Description modifiée");
            toUpdate.setStatut("active");
            toUpdate.setVenue("Théâtre Municipal");
            toUpdate.setLocation("Sousse");
            toUpdate.setGenre("Rock");
            es.modifier(toUpdate);
            System.out.println("Event modifié !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // --- SUPPRIMER (décommente si besoin) ---
        // try {
        //     es.supprimer(1);
        //     System.out.println("Event supprimé !");
        // } catch (SQLException e) {
        //     System.out.println(e.getMessage());
        // }

        // --- SEARCH avec STREAM ---
        try {
            System.out.println("=== Recherche par titre 'concert' ===");
            es.rechercherParTitre("concert").forEach(System.out::println);

            System.out.println("=== Recherche par statut 'active' ===");
            es.rechercherParStatut("active").forEach(System.out::println);

            System.out.println("=== Recherche par venue 'carthage' ===");
            es.rechercherParVenue("carthage").forEach(System.out::println);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // ==========================================
        // 2) RESERVATION — CRUD + SEARCH
        // ==========================================
        ReservationService rs = new ReservationService();

        // --- AJOUTER (event_id et user_id doivent exister) ---
        try {
            Reservation res = new Reservation(1, 1, 3);  // event_id=1, user_id=1, 3 places
            rs.ajouter(res);
            System.out.println("Reservation ajoutée !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // --- RECUPERER ---
        try {
            System.out.println("=== Toutes les réservations ===");
            rs.recuperer().forEach(System.out::println);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // --- MODIFIER ---
        try {
            Reservation toUpdate = new Reservation();
            toUpdate.setId(1);  // <-- adapte cet ID
            toUpdate.setEventId(1);
            toUpdate.setUserId(1);
            toUpdate.setNombrePlaces(5);
            toUpdate.setStatut("confirmed");
            rs.modifier(toUpdate);
            System.out.println("Reservation modifiée !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // --- SEARCH avec STREAM ---
        try {
            System.out.println("=== Recherche par statut 'confirmed' ===");
            rs.rechercherParStatut("confirmed").forEach(System.out::println);

            System.out.println("=== Recherche par event_id = 1 ===");
            rs.rechercherParEvent(1).forEach(System.out::println);

            System.out.println("=== Recherche par user_id = 1 ===");
            rs.rechercherParUser(1).forEach(System.out::println);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}

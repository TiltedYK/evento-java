package controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.Event;
import model.Reservation;
import model.User;
import service.EventService;
import service.GravatarService;
import service.ReferralHitService;
import service.ReservationService;
import service.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PROFILE module — user account & personal stats.
 *
 * APIs used:
 *   • GravatarService → Gravatar (generates avatar URL from email MD5 hash)
 *
 * Advanced features:
 *   1. Personal stats dashboard: total reservations, events attended, favourite genre, total seats
 *   2. Inline profile edit (first name, last name, email — saved to DB without page reload)
 */
public class FrontProfileController implements Initializable {

    @FXML private ImageView avatarView;
    @FXML private Label     displayName;
    @FXML private Label     emailLabel;
    @FXML private Label     gravatarNote;

    @FXML private Label statReservations;
    @FXML private Label statAttended;
    @FXML private Label statGenre;
    @FXML private Label statSeats;

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private Label     saveMessage;
    @FXML private Label     referralCodeLabel;
    @FXML private Label     referralCountLabel;

    private final ReservationService reservationService = new ReservationService();
    private final EventService       eventService       = new EventService();
    private final UserService        userService        = new UserService();
    private final ReferralHitService referralService    = new ReferralHitService();

    private User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user == null) {
            displayName.setText("Not logged in");
            return;
        }
        populateProfile();
        loadAvatarAsync();
        loadStatsAsync();
        loadReferralAsync();
    }

    // ── Profile display ────────────────────────────────────────────────

    private void populateProfile() {
        displayName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        emailLabel.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "No email set");
        firstNameField.setText(currentUser.getPrenom() != null ? currentUser.getPrenom() : "");
        lastNameField.setText(currentUser.getNom()    != null ? currentUser.getNom()    : "");
        emailField.setText(currentUser.getEmail()     != null ? currentUser.getEmail()  : "");
    }

    // ── Gravatar API (Advanced Feature 1 - visual identity) ───────────

    private void loadAvatarAsync() {
        String email = currentUser.getEmail() != null ? currentUser.getEmail() : "";
        gravatarNote.setText("Loading avatar via Gravatar…");
        new Thread(() -> {
            try {
                String url = GravatarService.getAvatarUrl(email, 96);
                Image img  = new Image(url, 96, 96, true, true, true);
                img.progressProperty().addListener((obs, o, n) -> {
                    if (n.doubleValue() >= 1.0) {
                        Platform.runLater(() -> {
                            avatarView.setImage(img);
                            gravatarNote.setText("Avatar by Gravatar — gravatar.com");
                        });
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> gravatarNote.setText("Avatar unavailable"));
            }
        }, "gravatar-loader").start();
    }

    // ── Stats (Advanced Feature 2 - personal analytics) ───────────────

    private void loadStatsAsync() {
        new Thread(() -> {
            try {
                List<Reservation> myRes = reservationService.recuperer().stream()
                        .filter(r -> r.getUserId() == currentUser.getId())
                        .collect(Collectors.toList());

                List<Event> allEvents = eventService.recuperer();

                long totalRes   = myRes.size();
                int  totalSeats = myRes.stream().mapToInt(Reservation::getNombrePlaces).sum();

                // Events attended = reservations for past events (not cancelled)
                long attended = myRes.stream()
                        .filter(r -> !"cancelled".equalsIgnoreCase(r.getStatut()))
                        .filter(r -> {
                            Event ev = allEvents.stream()
                                    .filter(e -> e.getId() == r.getEventId())
                                    .findFirst().orElse(null);
                            return ev != null && ev.getDateHeure() != null
                                    && ev.getDateHeure().isBefore(java.time.LocalDateTime.now());
                        })
                        .count();

                // Favourite genre — the genre the user reserved most
                Map<String, Long> genreCount = myRes.stream()
                        .map(r -> allEvents.stream()
                                .filter(e -> e.getId() == r.getEventId())
                                .findFirst().orElse(null))
                        .filter(Objects::nonNull)
                        .filter(e -> e.getGenre() != null && !e.getGenre().isBlank())
                        .collect(Collectors.groupingBy(Event::getGenre, Collectors.counting()));

                String topGenre = genreCount.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("—");

                final long  fRes      = totalRes;
                final long  fAttended = attended;
                final String fGenre   = topGenre;
                final int   fSeats    = totalSeats;

                Platform.runLater(() -> {
                    statReservations.setText(String.valueOf(fRes));
                    statAttended.setText(String.valueOf(fAttended));
                    statGenre.setText(fGenre);
                    statSeats.setText(String.valueOf(fSeats));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "profile-stats").start();
    }

    // ── Referral ───────────────────────────────────────────────────────

    private void loadReferralAsync() {
        if (currentUser == null) return;
        String code = referralService.generateReferralCode(currentUser.getId());
        if (referralCodeLabel != null) referralCodeLabel.setText(code);
        new Thread(() -> {
            int hits = referralService.countHitsForInfluencer(currentUser.getId());
            Platform.runLater(() -> {
                if (referralCountLabel != null) referralCountLabel.setText(String.valueOf(hits));
            });
        }, "referral-count").start();
    }

    @FXML void onCopyReferral() {
        if (currentUser == null || referralCodeLabel == null) return;
        String code = referralCodeLabel.getText();
        javafx.scene.input.ClipboardContent cb = new javafx.scene.input.ClipboardContent();
        cb.putString(code);
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(cb);
        saveMessage.setText("✅  Code " + code + " copied to clipboard!");
        saveMessage.setStyle("-fx-text-fill:#22C55E;");
        // Record a self-hit for demo
        referralService.recordHit(currentUser.getId(), "self-copy-" + System.currentTimeMillis(), null);
    }

    // ── Save profile ───────────────────────────────────────────────────

    @FXML void onSaveProfile() {
        if (currentUser == null) return;
        String fn = firstNameField.getText().trim();
        String ln = lastNameField.getText().trim();
        String em = emailField.getText().trim();

        if (fn.isEmpty() || ln.isEmpty()) {
            saveMessage.setText("First and last name are required.");
            saveMessage.setStyle("-fx-text-fill:#EF4444;");
            return;
        }

        currentUser.setPrenom(fn);
        currentUser.setNom(ln);
        currentUser.setEmail(em);

        try {
            userService.modifier(currentUser);
            displayName.setText(fn + " " + ln);
            emailLabel.setText(em);
            saveMessage.setText("✅  Profile updated!");
            saveMessage.setStyle("-fx-text-fill:#22C55E;");
            if (!em.isEmpty()) loadAvatarAsync(); // refresh gravatar with new email
        } catch (SQLException e) {
            saveMessage.setText("Save failed: " + e.getMessage());
            saveMessage.setStyle("-fx-text-fill:#EF4444;");
        }
    }
}

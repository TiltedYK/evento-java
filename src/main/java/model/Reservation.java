package model;

import java.time.LocalDateTime;

public class Reservation {

    private int id;
    private int eventId;
    private int userId;
    private int nombrePlaces;
    private String statut;
    private String confirmationToken;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;

    public Reservation() {
        this.statut = "pending";
        this.nombrePlaces = 1;
        this.createdAt = LocalDateTime.now();
    }

    public Reservation(int eventId, int userId, int nombrePlaces) {
        this.eventId = eventId;
        this.userId = userId;
        this.nombrePlaces = nombrePlaces;
        this.statut = "pending";
        this.createdAt = LocalDateTime.now();
    }

    public Reservation(int id, int eventId, int userId, int nombrePlaces, String statut, LocalDateTime createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
        this.nombrePlaces = nombrePlaces;
        this.statut = statut;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getNombrePlaces() { return nombrePlaces; }
    public void setNombrePlaces(int nombrePlaces) { this.nombrePlaces = nombrePlaces; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getConfirmationToken() { return confirmationToken; }
    public void setConfirmationToken(String confirmationToken) { this.confirmationToken = confirmationToken; }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", eventId=" + eventId +
                ", userId=" + userId +
                ", nombrePlaces=" + nombrePlaces +
                ", statut='" + statut + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

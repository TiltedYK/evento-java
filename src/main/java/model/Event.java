package model;

import java.time.LocalDateTime;

public class Event {

    private int id;
    private String titre;
    private LocalDateTime dateHeure;
    private int capacite;
    private String description;
    private String statut;
    private String venue;
    private String location;
    private Double latitude;
    private Double longitude;
    private String genre;
    private Integer artistId;
    private String imageFilename;
    private Integer createdById;

    public Event() {
    }

    public Event(String titre, LocalDateTime dateHeure, int capacite, String description, String statut, String venue) {
        this.titre = titre;
        this.dateHeure = dateHeure;
        this.capacite = capacite;
        this.description = description;
        this.statut = statut;
        this.venue = venue;
    }

    public Event(int id, String titre, LocalDateTime dateHeure, int capacite, String description, String statut, String venue) {
        this.id = id;
        this.titre = titre;
        this.dateHeure = dateHeure;
        this.capacite = capacite;
        this.description = description;
        this.statut = statut;
        this.venue = venue;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public LocalDateTime getDateHeure() { return dateHeure; }
    public void setDateHeure(LocalDateTime dateHeure) { this.dateHeure = dateHeure; }

    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public Integer getArtistId() { return artistId; }
    public void setArtistId(Integer artistId) { this.artistId = artistId; }

    public String getImageFilename() { return imageFilename; }
    public void setImageFilename(String imageFilename) { this.imageFilename = imageFilename; }

    public Integer getCreatedById() { return createdById; }
    public void setCreatedById(Integer createdById) { this.createdById = createdById; }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", dateHeure=" + dateHeure +
                ", capacite=" + capacite +
                ", statut='" + statut + '\'' +
                ", venue='" + venue + '\'' +
                ", genre='" + genre + '\'' +
                '}';
    }
}

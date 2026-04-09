package model;

import java.time.LocalDate;

public class User {

    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String numTelephone;
    private LocalDate dateNaissance;
    private String localisation;
    private String image;
    private String roles;
    private String password;
    private int banned;
    private int deleted;
    private int nblogin;

    public User() {
        this.banned = 0;
        this.deleted = 0;
        this.nblogin = 0;
        this.roles = "[\"ROLE_USER\"]";
    }

    public User(String nom, String prenom, String email, String numTelephone, String password) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.numTelephone = numTelephone;
        this.password = password;
        this.banned = 0;
        this.deleted = 0;
        this.nblogin = 0;
        this.roles = "[\"ROLE_USER\"]";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNumTelephone() { return numTelephone; }
    public void setNumTelephone(String numTelephone) { this.numTelephone = numTelephone; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getBanned() { return banned; }
    public void setBanned(int banned) { this.banned = banned; }

    public int getDeleted() { return deleted; }
    public void setDeleted(int deleted) { this.deleted = deleted; }

    public int getNblogin() { return nblogin; }
    public void setNblogin(int nblogin) { this.nblogin = nblogin; }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", numTelephone='" + numTelephone + '\'' +
                ", dateNaissance=" + dateNaissance +
                ", localisation='" + localisation + '\'' +
                ", banned=" + banned +
                '}';
    }
}

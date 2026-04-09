package test;

import model.*;
import service.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Main {

    public static void main(String[] args) {

        // ==========================================
        // 1) EVENT
        // ==========================================
        EventService es = new EventService();

        try {
            es.ajouter(new Event("Concert Rock", LocalDateTime.of(2026, 7, 15, 20, 0), 200, "Un super concert", "active", "Salle Carthage"));
            System.out.println("Event ajouté !");
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            System.out.println("=== Tous les événements ===");
            es.recuperer().forEach(System.out::println);
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            Event eu = new Event(1, "Concert UPDATED", LocalDateTime.of(2026, 8, 20, 21, 0), 300, "Description modifiée", "active", "Théâtre Municipal");
            es.modifier(eu);
            System.out.println("Event modifié !");
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            System.out.println("=== Stream: recherche par titre 'concert' ===");
            es.rechercherParTitre("concert").forEach(System.out::println);
            System.out.println("=== Stream: recherche par statut 'active' ===");
            es.rechercherParStatut("active").forEach(System.out::println);
            System.out.println("=== Stream: recherche par venue 'théâtre' ===");
            es.rechercherParVenue("théâtre").forEach(System.out::println);
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        // ==========================================
        // 2) RESERVATION
        // ==========================================
        ReservationService rs = new ReservationService();

        try {
            rs.ajouter(new Reservation(1, 1, 3));
            System.out.println("Reservation ajoutée !");
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            System.out.println("=== Toutes les réservations ===");
            rs.recuperer().forEach(System.out::println);
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            Reservation ru = new Reservation();
            ru.setId(1);
            ru.setEventId(1);
            ru.setUserId(1);
            ru.setNombrePlaces(5);
            ru.setStatut("confirmed");
            rs.modifier(ru);
            System.out.println("Reservation modifiée !");
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            System.out.println("=== Stream: recherche par statut 'confirmed' ===");
            rs.rechercherParStatut("confirmed").forEach(System.out::println);
            System.out.println("=== Stream: recherche par event_id=1 ===");
            rs.rechercherParEvent(1).forEach(System.out::println);
            System.out.println("=== Stream: recherche par user_id=1 ===");
            rs.rechercherParUser(1).forEach(System.out::println);
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        // ==========================================
        // 3) CATEGORY
        // ==========================================
        CategoryService cats = new CategoryService();

        try {
            cats.ajouter(new Category("T-Shirts", "T-shirts de concerts"));
            System.out.println("Category ajoutée !");
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            System.out.println("=== Toutes les catégories ===");
            cats.recuperer().forEach(System.out::println);
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            cats.modifier(new Category(1, "T-Shirts Updated", "Nouvelle description"));
            System.out.println("Category modifiée !");
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            System.out.println("=== Stream: recherche catégorie 'shirt' ===");
            cats.rechercherParNom("shirt").forEach(System.out::println);
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        // ==========================================
        // 4) PRODUCT
        // ==========================================
        ProductService prods = new ProductService();

        try {
            prods.ajouter(new Product("T-Shirt Rock", "T-shirt officiel", 29.99, 100, "Festival Store", 1));
            System.out.println("Product ajouté !");
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            System.out.println("=== Tous les produits ===");
            prods.recuperer().forEach(System.out::println);
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            Product pu = new Product();
            pu.setId(1);
            pu.setCategoryId(1);
            pu.setName("T-Shirt UPDATED");
            pu.setDescription("Modifié");
            pu.setPrice(34.99);
            pu.setStock(50);
            pu.setArtistName("Festival Store");
            pu.setAvailable(true);
            prods.modifier(pu);
            System.out.println("Product modifié !");
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            System.out.println("=== Stream: recherche produit 'shirt' ===");
            prods.rechercherParNom("shirt").forEach(System.out::println);
            System.out.println("=== Stream: recherche par artiste 'festival' ===");
            prods.rechercherParArtiste("festival").forEach(System.out::println);
            System.out.println("=== Stream: produits disponibles ===");
            prods.rechercherDisponibles().forEach(System.out::println);
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        // ==========================================
        // 5) POST
        // ==========================================
        PostService posts = new PostService();

        try {
            posts.ajouter(new Post(1, "Mon premier post", "Contenu de mon post sur le forum"));
            System.out.println("Post ajouté !");
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            System.out.println("=== Tous les posts ===");
            posts.recuperer().forEach(System.out::println);
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            Post postU = new Post();
            postU.setId(1);
            postU.setTitle("Post modifié");
            postU.setSlug("post-modifie");
            postU.setContent("Contenu modifié");
            posts.modifier(postU);
            System.out.println("Post modifié !");
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            System.out.println("=== Stream: recherche par titre 'post' ===");
            posts.rechercherParTitre("post").forEach(System.out::println);
            System.out.println("=== Stream: recherche par contenu 'contenu' ===");
            posts.rechercherParContenu("contenu").forEach(System.out::println);
            System.out.println("=== Stream: recherche par auteur id=1 ===");
            posts.rechercherParAuteur(1).forEach(System.out::println);
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        // ==========================================
        // 6) COMMENT
        // ==========================================
        CommentService coms = new CommentService();

        try {
            coms.ajouter(new Comment(1, 1, "Super post merci !"));
            System.out.println("Comment ajouté !");
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            System.out.println("=== Tous les commentaires ===");
            coms.recuperer().forEach(System.out::println);
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            Comment comU = new Comment();
            comU.setId(1);
            comU.setContent("Commentaire modifié");
            coms.modifier(comU);
            System.out.println("Comment modifié !");
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            System.out.println("=== Stream: recherche commentaire 'super' ===");
            coms.rechercherParContenu("super").forEach(System.out::println);
            System.out.println("=== Stream: recherche par post_id=1 ===");
            coms.rechercherParPost(1).forEach(System.out::println);
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        // ==========================================
        // 7) PARTNERSHIP REQUEST
        // ==========================================
        PartnershipRequestService prs = new PartnershipRequestService();

        try {
            prs.ajouter(new PartnershipRequest("Ahmed Ben Ali", "ahmed@test.com", "TechCorp", "12345678", "Nous voulons un partenariat"));
            System.out.println("PartnershipRequest ajouté !");
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            System.out.println("=== Toutes les demandes ===");
            prs.recuperer().forEach(System.out::println);
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            PartnershipRequest prU = new PartnershipRequest();
            prU.setId(1);
            prU.setContactName("Ahmed Updated");
            prU.setEmail("ahmed.updated@test.com");
            prU.setCompanyName("TechCorp Updated");
            prU.setPhone("99999999");
            prU.setMessage("Message modifié");
            prU.setStatus("confirmed");
            prs.modifier(prU);
            System.out.println("PartnershipRequest modifié !");
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            System.out.println("=== Stream: recherche par nom 'ahmed' ===");
            prs.rechercherParNom("ahmed").forEach(System.out::println);
            System.out.println("=== Stream: recherche par status 'pending' ===");
            prs.rechercherParStatus("pending").forEach(System.out::println);
            System.out.println("=== Stream: recherche par entreprise 'tech' ===");
            prs.rechercherParCompany("tech").forEach(System.out::println);
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        // ==========================================
        // 8) COLLABORATION
        // ==========================================
        CollaborationService cols = new CollaborationService();

        try {
            Collaboration collab = new Collaboration(1, "Bannière Sponsor", "image", "banner.png", "https://sponsor.com", "top", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30));
            cols.ajouter(collab);
            System.out.println("Collaboration ajoutée !");
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            System.out.println("=== Toutes les collaborations ===");
            cols.recuperer().forEach(System.out::println);
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            Collaboration colU = new Collaboration();
            colU.setId(1);
            colU.setPartnerId(1);
            colU.setTitle("Bannière Updated");
            colU.setType("image");
            colU.setFileName("banner_updated.png");
            colU.setLinkUrl("https://updated.com");
            colU.setPosition("bottom");
            colU.setStatus("approved");
            cols.modifier(colU);
            System.out.println("Collaboration modifiée !");
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            System.out.println("=== Stream: recherche par titre 'bannière' ===");
            cols.rechercherParTitre("bannière").forEach(System.out::println);
            System.out.println("=== Stream: recherche par status 'approved' ===");
            cols.rechercherParStatus("approved").forEach(System.out::println);
            System.out.println("=== Stream: recherche par type 'image' ===");
            cols.rechercherParType("image").forEach(System.out::println);
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        // ==========================================
        // 9) USER
        // ==========================================
        UserService us = new UserService();

        try {
            User u = new User("TestNom", "TestPrenom", "test@test.com", "12345678", "password123");
            u.setLocalisation("Tunis");
            u.setDateNaissance(LocalDate.of(2000, 5, 15));
            us.ajouter(u);
            System.out.println("User ajouté !");
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            System.out.println("=== Tous les utilisateurs ===");
            us.recuperer().forEach(System.out::println);
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            User uu = new User();
            uu.setId(1);
            uu.setNom("NomUpdated");
            uu.setPrenom("PrenomUpdated");
            uu.setEmail("updated@test.com");
            uu.setNumTelephone("99999999");
            uu.setLocalisation("Sousse");
            us.modifier(uu);
            System.out.println("User modifié !");
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        try {
            System.out.println("=== Stream: recherche par nom 'test' ===");
            us.rechercherParNom("test").forEach(System.out::println);
            System.out.println("=== Stream: recherche par email 'test' ===");
            us.rechercherParEmail("test").forEach(System.out::println);
            System.out.println("=== Stream: recherche par localisation 'tunis' ===");
            us.rechercherParLocalisation("tunis").forEach(System.out::println);
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        System.out.println("\n=== TERMINÉ ===");
    }
}

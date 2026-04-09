package test;

import model.Comment;
import model.Post;
import service.CommentService;
import service.PostService;

import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {

        // ==========================================
        // 1) POST — CRUD + SEARCH
        // ==========================================
        PostService ps = new PostService();

        // --- AJOUTER ---
        try {
            Post post = new Post(1, "Mon premier post", "Ceci est le contenu de mon premier post sur le forum");
            ps.ajouter(post);
            System.out.println("Post ajouté !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // --- RECUPERER ---
        try {
            System.out.println("=== Tous les posts ===");
            ps.recuperer().forEach(System.out::println);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // --- MODIFIER (adapte l'id) ---
        try {
            Post toUpdate = new Post();
            toUpdate.setId(1);  // <-- adapte cet ID
            toUpdate.setTitle("Post modifié");
            toUpdate.setSlug("post-modifie");
            toUpdate.setContent("Contenu modifié du post");
            ps.modifier(toUpdate);
            System.out.println("Post modifié !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // --- SEARCH avec STREAM ---
        try {
            System.out.println("=== Recherche par titre 'post' ===");
            ps.rechercherParTitre("post").forEach(System.out::println);

            System.out.println("=== Recherche par contenu 'contenu' ===");
            ps.rechercherParContenu("contenu").forEach(System.out::println);

            System.out.println("=== Recherche par auteur id=1 ===");
            ps.rechercherParAuteur(1).forEach(System.out::println);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // ==========================================
        // 2) COMMENT — CRUD + SEARCH
        // ==========================================
        CommentService coms = new CommentService();

        // --- AJOUTER (post_id et author_id doivent exister) ---
        try {
            Comment com = new Comment(1, 1, "Super post, merci pour le partage !");
            coms.ajouter(com);
            System.out.println("Comment ajouté !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // --- RECUPERER ---
        try {
            System.out.println("=== Tous les commentaires ===");
            coms.recuperer().forEach(System.out::println);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // --- MODIFIER ---
        try {
            Comment toUpdate = new Comment();
            toUpdate.setId(1);  // <-- adapte cet ID
            toUpdate.setContent("Commentaire modifié !");
            coms.modifier(toUpdate);
            System.out.println("Comment modifié !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // --- SEARCH avec STREAM ---
        try {
            System.out.println("=== Recherche commentaire 'super' ===");
            coms.rechercherParContenu("super").forEach(System.out::println);

            System.out.println("=== Recherche par post_id=1 ===");
            coms.rechercherParPost(1).forEach(System.out::println);

            System.out.println("=== Recherche par auteur id=1 ===");
            coms.rechercherParAuteur(1).forEach(System.out::println);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}

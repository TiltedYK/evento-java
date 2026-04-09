package test;

import model.Category;
import model.Product;
import service.CategoryService;
import service.ProductService;

import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {

        // ==========================================
        // 1) CATEGORY — CRUD + SEARCH
        // ==========================================
        CategoryService cs = new CategoryService();

        // --- AJOUTER ---
        try {
            cs.ajouter(new Category("T-Shirts", "T-shirts de concerts et événements"));
            cs.ajouter(new Category("Posters", "Affiches et posters d'artistes"));
            System.out.println("Categories ajoutées !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // --- RECUPERER ---
        try {
            System.out.println("=== Toutes les catégories ===");
            cs.recuperer().forEach(System.out::println);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // --- MODIFIER (adapte l'id) ---
        try {
            Category toUpdate = new Category(1, "T-Shirts Updated", "Nouvelle description");
            cs.modifier(toUpdate);
            System.out.println("Category modifiée !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // --- SEARCH avec STREAM ---
        try {
            System.out.println("=== Recherche catégorie 'shirt' ===");
            cs.rechercherParNom("shirt").forEach(System.out::println);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // ==========================================
        // 2) PRODUCT — CRUD + SEARCH
        // ==========================================
        ProductService ps = new ProductService();

        // --- AJOUTER (category_id doit exister) ---
        try {
            Product prod = new Product("T-Shirt Rock Festival", "T-shirt officiel du festival", 29.99, 100, "Festival Store", 1);
            ps.ajouter(prod);
            System.out.println("Product ajouté !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // --- RECUPERER ---
        try {
            System.out.println("=== Tous les produits ===");
            ps.recuperer().forEach(System.out::println);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // --- MODIFIER ---
        try {
            Product toUpdate = new Product();
            toUpdate.setId(1);  // <-- adapte cet ID
            toUpdate.setCategoryId(1);
            toUpdate.setName("T-Shirt Rock UPDATED");
            toUpdate.setDescription("Description modifiée");
            toUpdate.setPrice(34.99);
            toUpdate.setStock(50);
            toUpdate.setArtistName("Festival Store");
            toUpdate.setAvailable(true);
            ps.modifier(toUpdate);
            System.out.println("Product modifié !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // --- SEARCH avec STREAM ---
        try {
            System.out.println("=== Recherche produit 'rock' ===");
            ps.rechercherParNom("rock").forEach(System.out::println);

            System.out.println("=== Recherche par artiste 'festival' ===");
            ps.rechercherParArtiste("festival").forEach(System.out::println);

            System.out.println("=== Recherche produits disponibles ===");
            ps.rechercherDisponibles().forEach(System.out::println);

            System.out.println("=== Recherche par catégorie id=1 ===");
            ps.rechercherParCategorie(1).forEach(System.out::println);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}

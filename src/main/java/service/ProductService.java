package service;

import model.Product;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProductService implements IService<Product> {

    private Connection connection;

    public ProductService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Product p) throws SQLException {
        String sql = "INSERT INTO product (category_id, name, description, price, stock, image, artist_name, is_available, created_at, barcode) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, p.getCategoryId());
        ps.setString(2, p.getName());
        ps.setString(3, p.getDescription());
        ps.setDouble(4, p.getPrice());
        ps.setInt(5, p.getStock());
        ps.setString(6, p.getImage());
        ps.setString(7, p.getArtistName());
        ps.setBoolean(8, p.isAvailable());
        ps.setTimestamp(9, Timestamp.valueOf(p.getCreatedAt()));
        ps.setString(10, p.getBarcode());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Product p) throws SQLException {
        String sql = "UPDATE product SET category_id = ?, name = ?, description = ?, price = ?, stock = ?, image = ?, artist_name = ?, is_available = ?, barcode = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, p.getCategoryId());
        ps.setString(2, p.getName());
        ps.setString(3, p.getDescription());
        ps.setDouble(4, p.getPrice());
        ps.setInt(5, p.getStock());
        ps.setString(6, p.getImage());
        ps.setString(7, p.getArtistName());
        ps.setBoolean(8, p.isAvailable());
        ps.setString(9, p.getBarcode());
        ps.setInt(10, p.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM product WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Product> recuperer() throws SQLException {
        String sql = "SELECT * FROM product";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<Product> list = new ArrayList<>();
        while (rs.next()) {
            Product p = new Product();
            p.setId(rs.getInt("id"));
            p.setCategoryId(rs.getInt("category_id"));
            p.setName(rs.getString("name"));
            p.setDescription(rs.getString("description"));
            p.setPrice(rs.getDouble("price"));
            p.setStock(rs.getInt("stock"));
            p.setImage(rs.getString("image"));
            p.setArtistName(rs.getString("artist_name"));
            p.setAvailable(rs.getBoolean("is_available"));
            p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            p.setBarcode(rs.getString("barcode"));
            list.add(p);
        }
        return list;
    }

    // ========== STREAM-BASED SEARCH ==========

    public List<Product> rechercherParNom(String keyword) throws SQLException {
        return recuperer().stream()
                .filter(p -> p.getName().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Product> rechercherParArtiste(String keyword) throws SQLException {
        return recuperer().stream()
                .filter(p -> p.getArtistName().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Product> rechercherParCategorie(int categoryId) throws SQLException {
        return recuperer().stream()
                .filter(p -> p.getCategoryId() == categoryId)
                .collect(Collectors.toList());
    }

    public List<Product> rechercherDisponibles() throws SQLException {
        return recuperer().stream()
                .filter(Product::isAvailable)
                .collect(Collectors.toList());
    }
}

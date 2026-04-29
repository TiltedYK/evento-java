package service;

import model.Product;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class ProductService implements IService<Product> {

    private Connection connection;

    public ProductService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    private Set<String> tableColumns = null;

    private Set<String> columns() throws SQLException {
        if (tableColumns == null) {
            tableColumns = new HashSet<>();
            ResultSet rs = connection.getMetaData().getColumns(null, null, "product", null);
            while (rs.next()) tableColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
        }
        return tableColumns;
    }

    private boolean has(String col) throws SQLException {
        return columns().contains(col);
    }

    @Override
    public void ajouter(Product p) throws SQLException {
        List<String> cols = new ArrayList<>(Arrays.asList("category_id","name","description","price","stock","image","artist_name","is_available","created_at"));
        if (has("barcode")) cols.add("barcode");

        String sql = "INSERT INTO product (" + String.join(",", cols) + ") VALUES (" +
                String.join(",", Collections.nCopies(cols.size(), "?")) + ")";
        PreparedStatement ps = connection.prepareStatement(sql);
        int i = 1;
        ps.setInt(i++, p.getCategoryId());
        ps.setString(i++, p.getName());
        ps.setString(i++, p.getDescription());
        ps.setDouble(i++, p.getPrice());
        ps.setInt(i++, p.getStock());
        ps.setString(i++, p.getImage());
        ps.setString(i++, p.getArtistName());
        ps.setBoolean(i++, p.isAvailable());
        ps.setTimestamp(i++, p.getCreatedAt() != null ? Timestamp.valueOf(p.getCreatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
        if (has("barcode")) ps.setString(i, p.getBarcode());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Product p) throws SQLException {
        List<String> setCols = new ArrayList<>(Arrays.asList("category_id=?","name=?","description=?","price=?","stock=?","image=?","artist_name=?","is_available=?"));
        if (has("barcode")) setCols.add("barcode=?");

        String sql = "UPDATE product SET " + String.join(",", setCols) + " WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        int i = 1;
        ps.setInt(i++, p.getCategoryId());
        ps.setString(i++, p.getName());
        ps.setString(i++, p.getDescription());
        ps.setDouble(i++, p.getPrice());
        ps.setInt(i++, p.getStock());
        ps.setString(i++, p.getImage());
        ps.setString(i++, p.getArtistName());
        ps.setBoolean(i++, p.isAvailable());
        if (has("barcode")) ps.setString(i++, p.getBarcode());
        ps.setInt(i, p.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("DELETE FROM product WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Product> recuperer() throws SQLException {
        ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM product");
        Set<String> c = columns();
        List<Product> list = new ArrayList<>();
        while (rs.next()) {
            Product p = new Product();
            p.setId(rs.getInt("id"));
            p.setCategoryId(rs.getInt("category_id"));
            p.setName(rs.getString("name"));
            p.setDescription(rs.getString("description"));
            p.setPrice(rs.getDouble("price"));
            p.setStock(rs.getInt("stock"));
            if (c.contains("image"))       p.setImage(rs.getString("image"));
            if (c.contains("artist_name")) p.setArtistName(rs.getString("artist_name"));
            if (c.contains("is_available")) p.setAvailable(rs.getBoolean("is_available"));
            if (c.contains("created_at")) {
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) p.setCreatedAt(ts.toLocalDateTime());
            }
            if (c.contains("barcode")) p.setBarcode(rs.getString("barcode"));
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

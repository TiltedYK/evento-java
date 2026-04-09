package service;

import model.Category;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CategoryService implements IService<Category> {

    private Connection connection;

    public CategoryService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Category c) throws SQLException {
        String sql = "INSERT INTO category (name, description) VALUES (?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, c.getName());
        ps.setString(2, c.getDescription());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Category c) throws SQLException {
        String sql = "UPDATE category SET name = ?, description = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, c.getName());
        ps.setString(2, c.getDescription());
        ps.setInt(3, c.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM category WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Category> recuperer() throws SQLException {
        String sql = "SELECT * FROM category";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<Category> list = new ArrayList<>();
        while (rs.next()) {
            Category c = new Category();
            c.setId(rs.getInt("id"));
            c.setName(rs.getString("name"));
            c.setDescription(rs.getString("description"));
            list.add(c);
        }
        return list;
    }

    // ========== STREAM-BASED SEARCH ==========

    public List<Category> rechercherParNom(String keyword) throws SQLException {
        return recuperer().stream()
                .filter(c -> c.getName().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }
}

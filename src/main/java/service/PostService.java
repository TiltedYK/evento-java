package service;

import model.Post;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PostService implements IService<Post> {

    private Connection connection;

    public PostService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Post p) throws SQLException {
        String sql = "INSERT INTO post (author_id, title, slug, content, image, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, p.getAuthorId());
        ps.setString(2, p.getTitle());
        ps.setString(3, p.getSlug());
        ps.setString(4, p.getContent());
        ps.setString(5, p.getImage());
        ps.setTimestamp(6, Timestamp.valueOf(p.getCreatedAt()));
        ps.executeUpdate();
    }

    @Override
    public void modifier(Post p) throws SQLException {
        String sql = "UPDATE post SET title = ?, slug = ?, content = ?, image = ?, updated_at = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, p.getTitle());
        ps.setString(2, p.getSlug());
        ps.setString(3, p.getContent());
        ps.setString(4, p.getImage());
        ps.setTimestamp(5, Timestamp.valueOf(java.time.LocalDateTime.now()));
        ps.setInt(6, p.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM post WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Post> recuperer() throws SQLException {
        String sql = "SELECT * FROM post WHERE deleted_at IS NULL";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<Post> list = new ArrayList<>();
        while (rs.next()) {
            Post p = new Post();
            p.setId(rs.getInt("id"));
            p.setAuthorId(rs.getInt("author_id"));
            p.setTitle(rs.getString("title"));
            p.setSlug(rs.getString("slug"));
            p.setContent(rs.getString("content"));
            p.setImage(rs.getString("image"));
            p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            Timestamp updTs = rs.getTimestamp("updated_at");
            if (updTs != null) p.setUpdatedAt(updTs.toLocalDateTime());
            list.add(p);
        }
        return list;
    }

    // ========== STREAM-BASED SEARCH ==========

    public List<Post> rechercherParTitre(String keyword) throws SQLException {
        return recuperer().stream()
                .filter(p -> p.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Post> rechercherParContenu(String keyword) throws SQLException {
        return recuperer().stream()
                .filter(p -> p.getContent().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Post> rechercherParAuteur(int authorId) throws SQLException {
        return recuperer().stream()
                .filter(p -> p.getAuthorId() == authorId)
                .collect(Collectors.toList());
    }
}

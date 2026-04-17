package service;

import model.Comment;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommentService implements IService<Comment> {

    private Connection connection;

    public CommentService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Comment c) throws SQLException {
        String sql = "INSERT INTO comment (post_id, author_id, content, created_at) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, c.getPostId());
        ps.setInt(2, c.getAuthorId());
        ps.setString(3, c.getContent());
        ps.setTimestamp(4, Timestamp.valueOf(c.getCreatedAt()));
        ps.executeUpdate();
    }

    @Override
    public void modifier(Comment c) throws SQLException {
        String sql = "UPDATE comment SET content = ?, updated_at = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, c.getContent());
        ps.setTimestamp(2, Timestamp.valueOf(java.time.LocalDateTime.now()));
        ps.setInt(3, c.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM comment WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Comment> recuperer() throws SQLException {
        String sql = "SELECT * FROM comment WHERE deleted_at IS NULL";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<Comment> list = new ArrayList<>();
        while (rs.next()) {
            Comment c = new Comment();
            c.setId(rs.getInt("id"));
            c.setPostId(rs.getInt("post_id"));
            c.setAuthorId(rs.getInt("author_id"));
            c.setContent(rs.getString("content"));
            c.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            Timestamp updTs = rs.getTimestamp("updated_at");
            if (updTs != null) c.setUpdatedAt(updTs.toLocalDateTime());
            list.add(c);
        }
        return list;
    }

    // ========== STREAM-BASED SEARCH ==========

    public List<Comment> rechercherParContenu(String keyword) throws SQLException {
        return recuperer().stream()
                .filter(c -> c.getContent().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Comment> rechercherParPost(int postId) throws SQLException {
        return recuperer().stream()
                .filter(c -> c.getPostId() == postId)
                .collect(Collectors.toList());
    }

    public List<Comment> rechercherParAuteur(int authorId) throws SQLException {
        return recuperer().stream()
                .filter(c -> c.getAuthorId() == authorId)
                .collect(Collectors.toList());
    }
}

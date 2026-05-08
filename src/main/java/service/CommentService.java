package service;

import model.Comment;
import utils.MyDatabase;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class CommentService implements IService<Comment> {

    private Connection connection;
    private Set<String> tableColumns = null;

    public CommentService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    private Set<String> columns() throws SQLException {
        if (tableColumns == null) {
            tableColumns = new HashSet<>();
            ResultSet rs = connection.getMetaData().getColumns(null, null, "comment", null);
            while (rs.next()) tableColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
        }
        return tableColumns;
    }

    private boolean has(String col) throws SQLException { return columns().contains(col); }

    @Override
    public void ajouter(Comment c) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO comment (post_id, author_id, content, created_at) VALUES (?, ?, ?, ?)");
        ps.setInt(1, c.getPostId());
        ps.setInt(2, c.getAuthorId());
        ps.setString(3, c.getContent());
        ps.setTimestamp(4, Timestamp.valueOf(
                c.getCreatedAt() != null ? c.getCreatedAt() : java.time.LocalDateTime.now()));
        ps.executeUpdate();
    }

    @Override
    public void modifier(Comment c) throws SQLException {
        List<String> sets = new ArrayList<>(List.of("content=?"));
        if (has("updated_at")) sets.add("updated_at=?");
        PreparedStatement ps = connection.prepareStatement(
                "UPDATE comment SET " + String.join(",", sets) + " WHERE id=?");
        int i = 1;
        ps.setString(i++, c.getContent());
        if (has("updated_at")) ps.setTimestamp(i++, Timestamp.valueOf(java.time.LocalDateTime.now()));
        ps.setInt(i, c.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("DELETE FROM comment WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Comment> recuperer() throws SQLException {
        // avoid WHERE deleted_at IS NULL if that column doesn't exist
        String sql = has("deleted_at")
                ? "SELECT * FROM comment WHERE deleted_at IS NULL ORDER BY id DESC"
                : "SELECT * FROM comment ORDER BY id DESC";
        Set<String> cols = columns();
        ResultSet rs = connection.createStatement().executeQuery(sql);
        List<Comment> list = new ArrayList<>();
        while (rs.next()) {
            Comment c = new Comment();
            c.setId(rs.getInt("id"));
            c.setPostId(rs.getInt("post_id"));
            c.setAuthorId(rs.getInt("author_id"));
            c.setContent(rs.getString("content"));
            if (cols.contains("created_at")) {
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) c.setCreatedAt(ts.toLocalDateTime());
            }
            if (cols.contains("updated_at")) {
                Timestamp ut = rs.getTimestamp("updated_at");
                if (ut != null) c.setUpdatedAt(ut.toLocalDateTime());
            }
            list.add(c);
        }
        return list;
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

    public List<Comment> rechercherParContenu(String keyword) throws SQLException {
        return recuperer().stream()
                .filter(c -> c.getContent() != null && c.getContent().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }
}

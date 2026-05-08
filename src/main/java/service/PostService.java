package service;

import model.Post;
import utils.MyDatabase;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class PostService implements IService<Post> {

    private Connection connection;
    private Set<String> tableColumns = null;

    public PostService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    private Set<String> columns() throws SQLException {
        if (tableColumns == null) {
            tableColumns = new HashSet<>();
            ResultSet rs = connection.getMetaData().getColumns(null, null, "post", null);
            while (rs.next()) tableColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
        }
        return tableColumns;
    }

    private boolean has(String col) throws SQLException { return columns().contains(col); }

    @Override
    public void ajouter(Post p) throws SQLException {
        // Build INSERT dynamically — only include columns that exist
        List<String> cols = new ArrayList<>(Arrays.asList("author_id", "title", "content", "created_at"));
        if (has("slug"))  cols.add("slug");
        if (has("image")) cols.add("image");

        String sql = "INSERT INTO post (" + String.join(",", cols) + ") VALUES ("
                + String.join(",", Collections.nCopies(cols.size(), "?")) + ")";
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        int i = 1;
        ps.setInt(i++, p.getAuthorId());
        ps.setString(i++, p.getTitle());
        ps.setString(i++, p.getContent());
        ps.setTimestamp(i++, p.getCreatedAt() != null ? Timestamp.valueOf(p.getCreatedAt()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
        if (has("slug"))  ps.setString(i++, p.getSlug() != null ? p.getSlug() : slugify(p.getTitle()));
        if (has("image")) ps.setString(i,   p.getImage());
        ps.executeUpdate();

        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) p.setId(keys.getInt(1));
    }

    @Override
    public void modifier(Post p) throws SQLException {
        List<String> setCols = new ArrayList<>(Arrays.asList("title=?", "content=?"));
        if (has("slug"))       setCols.add("slug=?");
        if (has("image"))      setCols.add("image=?");
        if (has("updated_at")) setCols.add("updated_at=?");

        String sql = "UPDATE post SET " + String.join(",", setCols) + " WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        int i = 1;
        ps.setString(i++, p.getTitle());
        ps.setString(i++, p.getContent());
        if (has("slug"))       ps.setString(i++, p.getSlug());
        if (has("image"))      ps.setString(i++, p.getImage());
        if (has("updated_at")) ps.setTimestamp(i++, Timestamp.valueOf(java.time.LocalDateTime.now()));
        ps.setInt(i, p.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("DELETE FROM post WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Post> recuperer() throws SQLException {
        // Use plain SELECT * — avoid WHERE deleted_at IS NULL if that column doesn't exist
        String sql = has("deleted_at")
                ? "SELECT * FROM post WHERE deleted_at IS NULL ORDER BY id DESC"
                : "SELECT * FROM post ORDER BY id DESC";
        Set<String> cols = columns();
        ResultSet rs = connection.createStatement().executeQuery(sql);
        List<Post> list = new ArrayList<>();
        while (rs.next()) {
            Post p = new Post();
            p.setId(rs.getInt("id"));
            p.setAuthorId(rs.getInt("author_id"));
            p.setTitle(rs.getString("title"));
            p.setContent(rs.getString("content"));
            if (cols.contains("slug"))       p.setSlug(rs.getString("slug"));
            if (cols.contains("image"))      p.setImage(rs.getString("image"));
            if (cols.contains("created_at")) {
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) p.setCreatedAt(ts.toLocalDateTime());
            }
            if (cols.contains("updated_at")) {
                Timestamp ut = rs.getTimestamp("updated_at");
                if (ut != null) p.setUpdatedAt(ut.toLocalDateTime());
            }
            list.add(p);
        }
        return list;
    }

    public List<Post> rechercherParTitre(String keyword) throws SQLException {
        return recuperer().stream()
                .filter(p -> p.getTitle() != null && p.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Post> rechercherParContenu(String keyword) throws SQLException {
        return recuperer().stream()
                .filter(p -> p.getContent() != null && p.getContent().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Post> rechercherParAuteur(int authorId) throws SQLException {
        return recuperer().stream()
                .filter(p -> p.getAuthorId() == authorId)
                .collect(Collectors.toList());
    }

    private String slugify(String title) {
        if (title == null) return "";
        return title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}

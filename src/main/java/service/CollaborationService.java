package service;

import model.Collaboration;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CollaborationService implements IService<Collaboration> {

    private Connection connection;

    public CollaborationService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Collaboration c) throws SQLException {
        String sql = "INSERT INTO collaboration (partner_id, title, type, file_name, link_url, position, status, start_date, end_date, created_at, view_count, width, height, price) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, c.getPartnerId());
        ps.setString(2, c.getTitle());
        ps.setString(3, c.getType());
        ps.setString(4, c.getFileName());
        ps.setString(5, c.getLinkUrl());
        ps.setString(6, c.getPosition());
        ps.setString(7, c.getStatus());
        ps.setDate(8, c.getStartDate() != null ? Date.valueOf(c.getStartDate()) : null);
        ps.setDate(9, c.getEndDate() != null ? Date.valueOf(c.getEndDate()) : null);
        ps.setTimestamp(10, Timestamp.valueOf(c.getCreatedAt()));
        ps.setInt(11, c.getViewCount());
        if (c.getWidth() != null) ps.setInt(12, c.getWidth()); else ps.setNull(12, Types.INTEGER);
        if (c.getHeight() != null) ps.setInt(13, c.getHeight()); else ps.setNull(13, Types.INTEGER);
        if (c.getPrice() != null) ps.setDouble(14, c.getPrice()); else ps.setNull(14, Types.DOUBLE);
        ps.executeUpdate();
    }

    @Override
    public void modifier(Collaboration c) throws SQLException {
        String sql = "UPDATE collaboration SET partner_id = ?, title = ?, type = ?, file_name = ?, link_url = ?, position = ?, status = ?, start_date = ?, end_date = ?, width = ?, height = ?, price = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, c.getPartnerId());
        ps.setString(2, c.getTitle());
        ps.setString(3, c.getType());
        ps.setString(4, c.getFileName());
        ps.setString(5, c.getLinkUrl());
        ps.setString(6, c.getPosition());
        ps.setString(7, c.getStatus());
        ps.setDate(8, c.getStartDate() != null ? Date.valueOf(c.getStartDate()) : null);
        ps.setDate(9, c.getEndDate() != null ? Date.valueOf(c.getEndDate()) : null);
        if (c.getWidth() != null) ps.setInt(10, c.getWidth()); else ps.setNull(10, Types.INTEGER);
        if (c.getHeight() != null) ps.setInt(11, c.getHeight()); else ps.setNull(11, Types.INTEGER);
        if (c.getPrice() != null) ps.setDouble(12, c.getPrice()); else ps.setNull(12, Types.DOUBLE);
        ps.setInt(13, c.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM collaboration WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Collaboration> recuperer() throws SQLException {
        String sql = "SELECT * FROM collaboration";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<Collaboration> list = new ArrayList<>();
        while (rs.next()) {
            Collaboration c = new Collaboration();
            c.setId(rs.getInt("id"));
            c.setPartnerId(rs.getInt("partner_id"));
            c.setTitle(rs.getString("title"));
            c.setType(rs.getString("type"));
            c.setFileName(rs.getString("file_name"));
            c.setLinkUrl(rs.getString("link_url"));
            c.setPosition(rs.getString("position"));
            c.setStatus(rs.getString("status"));
            Date sd = rs.getDate("start_date");
            if (sd != null) c.setStartDate(sd.toLocalDate());
            Date ed = rs.getDate("end_date");
            if (ed != null) c.setEndDate(ed.toLocalDate());
            c.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            c.setViewCount(rs.getInt("view_count"));
            c.setWidth(rs.getObject("width") != null ? rs.getInt("width") : null);
            c.setHeight(rs.getObject("height") != null ? rs.getInt("height") : null);
            c.setPrice(rs.getObject("price") != null ? rs.getDouble("price") : null);
            list.add(c);
        }
        return list;
    }

    // ========== STREAM-BASED SEARCH ==========

    public List<Collaboration> rechercherParTitre(String keyword) throws SQLException {
        return recuperer().stream()
                .filter(c -> c.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Collaboration> rechercherParStatus(String status) throws SQLException {
        return recuperer().stream()
                .filter(c -> c.getStatus().equalsIgnoreCase(status))
                .collect(Collectors.toList());
    }

    public List<Collaboration> rechercherParType(String type) throws SQLException {
        return recuperer().stream()
                .filter(c -> c.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }
}

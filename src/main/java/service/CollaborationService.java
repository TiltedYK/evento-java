package service;

import model.Collaboration;
import utils.MyDatabase;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class CollaborationService implements IService<Collaboration> {

    private Connection connection;
    private Set<String> tableColumns = null;

    public CollaborationService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    private Set<String> columns() throws SQLException {
        if (tableColumns == null) {
            tableColumns = new HashSet<>();
            ResultSet rs = connection.getMetaData().getColumns(null, null, "collaboration", null);
            while (rs.next()) tableColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
        }
        return tableColumns;
    }

    private boolean has(String col) throws SQLException { return columns().contains(col); }

    @Override
    public void ajouter(Collaboration c) throws SQLException {
        boolean hasRef = has("referral_code");
        String sql = "INSERT INTO collaboration (partner_id, title, type, file_name, link_url, position, status, start_date, end_date, created_at, view_count, width, height, price" 
                     + (hasRef ? ", referral_code" : "") + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" + (hasRef ? ", ?" : "") + ")";
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
        if (hasRef) ps.setString(15, c.getReferralCode());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Collaboration c) throws SQLException {
        boolean hasRef = has("referral_code");
        String sql = "UPDATE collaboration SET partner_id = ?, title = ?, type = ?, file_name = ?, link_url = ?, position = ?, status = ?, start_date = ?, end_date = ?, width = ?, height = ?, price = ?"
                     + (hasRef ? ", referral_code = ?" : "") + " WHERE id = ?";
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
        if (hasRef) {
            ps.setString(13, c.getReferralCode());
            ps.setInt(14, c.getId());
        } else {
            ps.setInt(13, c.getId());
        }
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
        ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM collaboration");
        Set<String> cols = columns();
        List<Collaboration> list = new ArrayList<>();
        while (rs.next()) {
            Collaboration c = new Collaboration();
            c.setId(rs.getInt("id"));
            if (cols.contains("partner_id"))  c.setPartnerId(rs.getInt("partner_id"));
            if (cols.contains("title"))       c.setTitle(rs.getString("title"));
            if (cols.contains("type"))        c.setType(rs.getString("type"));
            if (cols.contains("file_name"))   c.setFileName(rs.getString("file_name"));
            if (cols.contains("link_url"))    c.setLinkUrl(rs.getString("link_url"));
            if (cols.contains("position"))    c.setPosition(rs.getString("position"));
            if (cols.contains("status"))      c.setStatus(rs.getString("status"));
            if (cols.contains("start_date")) { java.sql.Date sd = rs.getDate("start_date"); if (sd != null) c.setStartDate(sd.toLocalDate()); }
            if (cols.contains("end_date"))   { java.sql.Date ed = rs.getDate("end_date");   if (ed != null) c.setEndDate(ed.toLocalDate()); }
            if (cols.contains("created_at")) { Timestamp ts = rs.getTimestamp("created_at"); if (ts != null) c.setCreatedAt(ts.toLocalDateTime()); }
            if (cols.contains("view_count")) c.setViewCount(rs.getInt("view_count"));
            if (cols.contains("width"))      c.setWidth(rs.getObject("width")  != null ? rs.getInt("width")    : null);
            if (cols.contains("height"))     c.setHeight(rs.getObject("height") != null ? rs.getInt("height")  : null);
            if (cols.contains("price"))      c.setPrice(rs.getObject("price")   != null ? rs.getDouble("price"): null);
            if (cols.contains("referral_code")) c.setReferralCode(rs.getString("referral_code"));
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

    // ========== ADVANCED BUSINESS FUNCTIONALITY ==========

    /**
     * Calculates the estimated price for a collaboration based on its type, position, and duration.
     */
    public double calculateEstimatedPrice(String type, String position, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return 0.0;
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days == 0) days = 1; // Minimum 1 day

        double baseDailyRate = 10.0; // Base rate per day
        double typeMultiplier = "video".equalsIgnoreCase(type) ? 2.5 : 1.0;
        double positionMultiplier = "top".equalsIgnoreCase(position) ? 1.5 : 1.0; // Top is more expensive than Sidebar/bottom

        return baseDailyRate * days * typeMultiplier * positionMultiplier;
    }
}

package service;

import model.Reservation;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class ReservationService implements IService<Reservation> {

    private Connection connection;
    private Set<String> tableColumns = null;

    public ReservationService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    private Set<String> columns() throws SQLException {
        if (tableColumns == null) {
            tableColumns = new HashSet<>();
            ResultSet rs = connection.getMetaData().getColumns(null, null, "reservation", null);
            while (rs.next()) tableColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
        }
        return tableColumns;
    }

    private boolean has(String col) throws SQLException { return columns().contains(col); }

    @Override
    public void ajouter(Reservation r) throws SQLException {
        String sql = "INSERT INTO reservation (event_id, user_id, nombre_places, statut, created_at) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, r.getEventId());
        ps.setInt(2, r.getUserId());
        ps.setInt(3, r.getNombrePlaces());
        ps.setString(4, r.getStatut());
        ps.setTimestamp(5, Timestamp.valueOf(r.getCreatedAt()));
        ps.executeUpdate();
    }

    @Override
    public void modifier(Reservation r) throws SQLException {
        String sql = "UPDATE reservation SET event_id = ?, user_id = ?, nombre_places = ?, statut = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, r.getEventId());
        ps.setInt(2, r.getUserId());
        ps.setInt(3, r.getNombrePlaces());
        ps.setString(4, r.getStatut());
        ps.setInt(5, r.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM reservation WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Reservation> recuperer() throws SQLException {
        String sql = "SELECT * FROM reservation";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        Set<String> c = columns();
        List<Reservation> list = new ArrayList<>();
        while (rs.next()) {
            Reservation r = new Reservation();
            r.setId(rs.getInt("id"));
            r.setEventId(rs.getInt("event_id"));
            r.setUserId(rs.getInt("user_id"));
            r.setNombrePlaces(rs.getInt("nombre_places"));
            r.setStatut(rs.getString("statut"));
            if (c.contains("created_at")) {
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) r.setCreatedAt(ts.toLocalDateTime());
            }
            if (has("confirmed_at")) {
                Timestamp ct = rs.getTimestamp("confirmed_at");
                if (ct != null) r.setConfirmedAt(ct.toLocalDateTime());
            }
            if (has("confirmation_token"))
                r.setConfirmationToken(rs.getString("confirmation_token"));
            list.add(r);
        }
        return list;
    }

    // ========== STREAM-BASED SEARCH ==========

    public List<Reservation> rechercherParStatut(String statut) throws SQLException {
        return recuperer().stream()
                .filter(r -> r.getStatut().equalsIgnoreCase(statut))
                .collect(Collectors.toList());
    }

    public List<Reservation> rechercherParEvent(int eventId) throws SQLException {
        return recuperer().stream()
                .filter(r -> r.getEventId() == eventId)
                .collect(Collectors.toList());
    }

    public List<Reservation> rechercherParUser(int userId) throws SQLException {
        return recuperer().stream()
                .filter(r -> r.getUserId() == userId)
                .collect(Collectors.toList());
    }

    public int countByEventId(int eventId) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "SELECT COALESCE(SUM(nombre_places), 0) FROM reservation WHERE event_id = ?");
        ps.setInt(1, eventId);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    public int countTotalByUser(int userId) throws SQLException {
        return (int) recuperer().stream()
                .filter(r -> r.getUserId() == userId)
                .mapToInt(r -> r.getNombrePlaces())
                .sum();
    }
}

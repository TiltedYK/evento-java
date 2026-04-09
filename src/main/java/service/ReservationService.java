package service;

import model.Reservation;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReservationService implements IService<Reservation> {

    private Connection connection;

    public ReservationService() {
        connection = MyDatabase.getInstance().getConnection();
    }

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

        List<Reservation> list = new ArrayList<>();
        while (rs.next()) {
            Reservation r = new Reservation();
            r.setId(rs.getInt("id"));
            r.setEventId(rs.getInt("event_id"));
            r.setUserId(rs.getInt("user_id"));
            r.setNombrePlaces(rs.getInt("nombre_places"));
            r.setStatut(rs.getString("statut"));
            Timestamp confirmedTs = rs.getTimestamp("confirmed_at");
            if (confirmedTs != null) r.setConfirmedAt(confirmedTs.toLocalDateTime());
            r.setConfirmationToken(rs.getString("confirmation_token"));
            r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
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
}

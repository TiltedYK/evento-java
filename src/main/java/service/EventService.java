package service;

import model.Event;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EventService implements IService<Event> {

    private Connection connection;

    public EventService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Event e) throws SQLException {
        String sql = "INSERT INTO event (titre, date_heure, capacite, description, statut, venue, location, latitude, longitude, genre, artist_id, image_filename, created_by_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, e.getTitre());
        ps.setTimestamp(2, Timestamp.valueOf(e.getDateHeure()));
        ps.setInt(3, e.getCapacite());
        ps.setString(4, e.getDescription());
        ps.setString(5, e.getStatut());
        ps.setString(6, e.getVenue());
        ps.setString(7, e.getLocation());
        if (e.getLatitude() != null) ps.setDouble(8, e.getLatitude()); else ps.setNull(8, Types.DOUBLE);
        if (e.getLongitude() != null) ps.setDouble(9, e.getLongitude()); else ps.setNull(9, Types.DOUBLE);
        ps.setString(10, e.getGenre());
        if (e.getArtistId() != null) ps.setInt(11, e.getArtistId()); else ps.setNull(11, Types.INTEGER);
        ps.setString(12, e.getImageFilename());
        if (e.getCreatedById() != null) ps.setInt(13, e.getCreatedById()); else ps.setNull(13, Types.INTEGER);
        ps.executeUpdate();
    }

    @Override
    public void modifier(Event e) throws SQLException {
        String sql = "UPDATE event SET titre = ?, date_heure = ?, capacite = ?, description = ?, statut = ?, venue = ?, location = ?, genre = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, e.getTitre());
        ps.setTimestamp(2, Timestamp.valueOf(e.getDateHeure()));
        ps.setInt(3, e.getCapacite());
        ps.setString(4, e.getDescription());
        ps.setString(5, e.getStatut());
        ps.setString(6, e.getVenue());
        ps.setString(7, e.getLocation());
        ps.setString(8, e.getGenre());
        ps.setInt(9, e.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM event WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Event> recuperer() throws SQLException {
        String sql = "SELECT * FROM event";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<Event> list = new ArrayList<>();
        while (rs.next()) {
            Event e = new Event();
            e.setId(rs.getInt("id"));
            e.setTitre(rs.getString("titre"));
            e.setDateHeure(rs.getTimestamp("date_heure").toLocalDateTime());
            e.setCapacite(rs.getInt("capacite"));
            e.setDescription(rs.getString("description"));
            e.setStatut(rs.getString("statut"));
            e.setVenue(rs.getString("venue"));
            e.setLocation(rs.getString("location"));
            e.setLatitude(rs.getObject("latitude") != null ? rs.getDouble("latitude") : null);
            e.setLongitude(rs.getObject("longitude") != null ? rs.getDouble("longitude") : null);
            e.setGenre(rs.getString("genre"));
            e.setArtistId(rs.getObject("artist_id") != null ? rs.getInt("artist_id") : null);
            e.setImageFilename(rs.getString("image_filename"));
            e.setCreatedById(rs.getObject("created_by_id") != null ? rs.getInt("created_by_id") : null);
            list.add(e);
        }
        return list;
    }

    // ========== STREAM-BASED SEARCH ==========

    public List<Event> rechercherParTitre(String keyword) throws SQLException {
        return recuperer().stream()
                .filter(e -> e.getTitre().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Event> rechercherParStatut(String statut) throws SQLException {
        return recuperer().stream()
                .filter(e -> e.getStatut().equalsIgnoreCase(statut))
                .collect(Collectors.toList());
    }

    public List<Event> rechercherParVenue(String keyword) throws SQLException {
        return recuperer().stream()
                .filter(e -> e.getVenue().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }
}

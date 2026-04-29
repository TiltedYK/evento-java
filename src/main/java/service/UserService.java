package service;

import model.User;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UserService implements IService<User> {

    private Connection connection;

    public UserService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(User u) throws SQLException {
        String sql = "INSERT INTO user (nom, prenom, email, num_telephone, date_naissance, localisation, image, roles, password, banned, deleted, nblogin) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getNumTelephone());
        ps.setDate(5, u.getDateNaissance() != null ? Date.valueOf(u.getDateNaissance()) : null);
        ps.setString(6, u.getLocalisation());
        ps.setString(7, u.getImage());
        ps.setString(8, u.getRoles());
        ps.setString(9, u.getPassword());
        ps.setInt(10, u.getBanned());
        ps.setInt(11, u.getDeleted());
        ps.setInt(12, u.getNblogin());
        ps.executeUpdate();
    }

    @Override
    public void modifier(User u) throws SQLException {
        String sql = "UPDATE user SET nom = ?, prenom = ?, email = ?, num_telephone = ?, date_naissance = ?, localisation = ?, roles = ?, image = ?, banned = ?, deleted = ?, nblogin = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getNumTelephone());
        ps.setDate(5, u.getDateNaissance() != null ? Date.valueOf(u.getDateNaissance()) : null);
        ps.setString(6, u.getLocalisation());
        ps.setString(7, u.getRoles());
        ps.setString(8, u.getImage());
        ps.setInt(9, u.getBanned());
        ps.setInt(10, u.getDeleted());
        ps.setInt(11, u.getNblogin());
        ps.setInt(12, u.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM user WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<User> recuperer() throws SQLException {
        String sql = "SELECT * FROM user";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<User> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapUser(rs));
        }
        return list;
    }

    public User recupererParId(int id) throws SQLException {
        String sql = "SELECT * FROM user WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapUser(rs);
        }
        return null;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setNumTelephone(rs.getString("num_telephone"));
        Date dn = rs.getDate("date_naissance");
        if (dn != null) u.setDateNaissance(dn.toLocalDate());
        u.setLocalisation(rs.getString("localisation"));
        u.setImage(rs.getString("image"));
        u.setRoles(rs.getString("roles"));
        u.setPassword(rs.getString("password"));
        u.setBanned(rs.getInt("banned"));
        u.setDeleted(rs.getInt("deleted"));
        u.setNblogin(rs.getInt("nblogin"));
        return u;
    }

    // ========== STREAM-BASED SEARCH ==========

    public List<User> rechercherParNom(String keyword) throws SQLException {
        return recuperer().stream()
                .filter(u -> u.getNom().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<User> rechercherParEmail(String keyword) throws SQLException {
        return recuperer().stream()
                .filter(u -> u.getEmail().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<User> rechercherParLocalisation(String keyword) throws SQLException {
        return recuperer().stream()
                .filter(u -> u.getLocalisation() != null && u.getLocalisation().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    /** Returns all users whose roles JSON contains "ROLE_ARTIST". */
    public List<User> getArtists() throws SQLException {
        return recuperer().stream()
                .filter(u -> u.getRoles() != null && u.getRoles().contains("ROLE_ARTIST"))
                .filter(u -> u.getBanned() == 0 && u.getDeleted() == 0)
                .collect(Collectors.toList());
    }
}

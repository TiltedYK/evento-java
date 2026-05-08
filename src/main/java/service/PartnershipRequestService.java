package service;

import model.PartnershipRequest;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PartnershipRequestService implements IService<PartnershipRequest> {

    private Connection connection;

    public PartnershipRequestService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(PartnershipRequest p) throws SQLException {
        String sql = "INSERT INTO partnership_request (contact_name, email, company_name, phone, message, status, created_at, user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, p.getContactName());
        ps.setString(2, p.getEmail());
        ps.setString(3, p.getCompanyName());
        ps.setString(4, p.getPhone());
        ps.setString(5, p.getMessage());
        ps.setString(6, p.getStatus());
        ps.setTimestamp(7, Timestamp.valueOf(p.getCreatedAt()));
        if (p.getUserId() != null) ps.setInt(8, p.getUserId()); else ps.setNull(8, Types.INTEGER);
        ps.executeUpdate();
    }

    @Override
    public void modifier(PartnershipRequest p) throws SQLException {
        String sql = "UPDATE partnership_request SET contact_name = ?, email = ?, company_name = ?, phone = ?, message = ?, status = ?, user_id = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, p.getContactName());
        ps.setString(2, p.getEmail());
        ps.setString(3, p.getCompanyName());
        ps.setString(4, p.getPhone());
        ps.setString(5, p.getMessage());
        ps.setString(6, p.getStatus());
        if (p.getUserId() != null) ps.setInt(7, p.getUserId()); else ps.setNull(7, Types.INTEGER);
        ps.setInt(8, p.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM partnership_request WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<PartnershipRequest> recuperer() throws SQLException {
        String sql = "SELECT * FROM partnership_request";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<PartnershipRequest> list = new ArrayList<>();
        while (rs.next()) {
            PartnershipRequest p = new PartnershipRequest();
            p.setId(rs.getInt("id"));
            p.setUserId(rs.getObject("user_id") != null ? rs.getInt("user_id") : null);
            p.setStatus(rs.getString("status"));
            p.setContactName(rs.getString("contact_name"));
            p.setEmail(rs.getString("email"));
            p.setCompanyName(rs.getString("company_name"));
            p.setPhone(rs.getString("phone"));
            p.setMessage(rs.getString("message"));
            p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            list.add(p);
        }
        return list;
    }

    // ========== STREAM-BASED SEARCH ==========

    public List<PartnershipRequest> rechercherParNom(String keyword) throws SQLException {
        return recuperer().stream()
                .filter(p -> p.getContactName().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<PartnershipRequest> rechercherParStatus(String status) throws SQLException {
        return recuperer().stream()
                .filter(p -> p.getStatus().equalsIgnoreCase(status))
                .collect(Collectors.toList());
    }

    public List<PartnershipRequest> rechercherParCompany(String keyword) throws SQLException {
        return recuperer().stream()
                .filter(p -> p.getCompanyName().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }
}

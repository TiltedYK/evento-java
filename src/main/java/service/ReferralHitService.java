package service;

import model.ReferralHit;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReferralHitService implements IService<ReferralHit> {

    private Connection connection;

    public ReferralHitService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(ReferralHit r) throws SQLException {
        String sql = "INSERT INTO referral_hit (influencer_id, session_id, visited_at, referred_user_id) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, r.getInfluencerId());
        ps.setString(2, r.getSessionId());
        ps.setTimestamp(3, Timestamp.valueOf(r.getVisitedAt()));
        
        if (r.getReferredUserId() != null) {
            ps.setInt(4, r.getReferredUserId());
        } else {
            ps.setNull(4, Types.INTEGER);
        }
        
        ps.executeUpdate();
    }

    @Override
    public void modifier(ReferralHit r) throws SQLException {
        String sql = "UPDATE referral_hit SET influencer_id = ?, session_id = ?, visited_at = ?, referred_user_id = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, r.getInfluencerId());
        ps.setString(2, r.getSessionId());
        ps.setTimestamp(3, Timestamp.valueOf(r.getVisitedAt()));
        
        if (r.getReferredUserId() != null) {
            ps.setInt(4, r.getReferredUserId());
        } else {
            ps.setNull(4, Types.INTEGER);
        }
        
        ps.setInt(5, r.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM referral_hit WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<ReferralHit> recuperer() throws SQLException {
        String sql = "SELECT * FROM referral_hit";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<ReferralHit> list = new ArrayList<>();
        while (rs.next()) {
            ReferralHit r = new ReferralHit();
            r.setId(rs.getInt("id"));
            r.setInfluencerId(rs.getInt("influencer_id"));
            r.setSessionId(rs.getString("session_id"));
            r.setVisitedAt(rs.getTimestamp("visited_at").toLocalDateTime());
            
            int referredId = rs.getInt("referred_user_id");
            if (!rs.wasNull()) {
                r.setReferredUserId(referredId);
            }
            
            list.add(r);
        }
        return list;
    }

    // ========== STREAM-BASED SEARCH ==========

    public List<ReferralHit> rechercherParSessionId(String keyword) throws SQLException {
        return recuperer().stream()
                .filter(r -> r.getSessionId() != null && r.getSessionId().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<ReferralHit> rechercherParInfluencerId(int influencerId) throws SQLException {
        return recuperer().stream()
                .filter(r -> r.getInfluencerId() == influencerId)
                .collect(Collectors.toList());
    }
}

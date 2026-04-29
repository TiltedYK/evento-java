package service;

import model.ReferralHit;
import utils.MyDatabase;

import java.sql.*;
import java.util.*;

/**
 * Tracks ad impressions & clicks via the referral_hit table.
 *
 * Mapping used:
 *   influencer_id   = collaborationId  (the ad being tracked)
 *   session_id      = "impression" | "click" | "referral"
 *   referred_user_id = userId who triggered the action
 */
public class ReferralHitService {

    private Connection connection;
    private Set<String> tableColumns = null;

    public ReferralHitService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    private Set<String> columns() throws SQLException {
        if (tableColumns == null) {
            tableColumns = new HashSet<>();
            ResultSet rs = connection.getMetaData().getColumns(null, null, "referral_hit", null);
            while (rs.next()) tableColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
        }
        return tableColumns;
    }

    private boolean tableExists() {
        try { return !columns().isEmpty(); } catch (Exception e) { return false; }
    }

    // ── Core record ───────────────────────────────────────────────────

    public void recordHit(int influencerId, String sessionId, Integer referredUserId) {
        if (!tableExists()) return;
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO referral_hit (influencer_id, session_id, visited_at, referred_user_id) VALUES (?,?,?,?)");
            ps.setInt(1, influencerId);
            ps.setString(2, sessionId);
            ps.setTimestamp(3, Timestamp.valueOf(java.time.LocalDateTime.now()));
            if (referredUserId != null) ps.setInt(4, referredUserId); else ps.setNull(4, Types.INTEGER);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    // ── Ad Analytics ──────────────────────────────────────────────────

    /** Record that a user saw this ad (impression). */
    public void recordImpression(int collabId, Integer userId) {
        recordHit(collabId, "impression", userId);
    }

    /** Record that a user clicked "Visit" on this ad (click). */
    public void recordClick(int collabId, Integer userId) {
        recordHit(collabId, "click", userId);
    }

    /** Count impressions for a collaboration ad. */
    public int getImpressions(int collabId) {
        return countWhere(collabId, "impression");
    }

    /** Count clicks for a collaboration ad. */
    public int getClicks(int collabId) {
        return countWhere(collabId, "click");
    }

    private int countWhere(int collabId, String sessionType) {
        if (!tableExists()) return 0;
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM referral_hit WHERE influencer_id=? AND session_id=?");
            ps.setInt(1, collabId);
            ps.setString(2, sessionType);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) { return 0; }
    }

    /**
     * Compute ad effectiveness as a letter grade based on CTR.
     *   CTR = clicks / impressions × 100
     *   A+  ≥ 8%  |  A ≥ 5%  |  B ≥ 2%  |  C ≥ 0.5%  |  D < 0.5%
     */
    public String effectivenessGrade(int collabId) {
        int imp    = getImpressions(collabId);
        int clicks = getClicks(collabId);
        if (imp == 0) return "—";
        double ctr = (double) clicks / imp * 100;
        if (ctr >= 8)   return "A+";
        if (ctr >= 5)   return "A";
        if (ctr >= 2)   return "B";
        if (ctr >= 0.5) return "C";
        return "D";
    }

    public double getCTR(int collabId) {
        int imp = getImpressions(collabId);
        if (imp == 0) return 0;
        return (double) getClicks(collabId) / imp * 100;
    }

    // ── IService compatibility (used by admin ReferralHitController) ──

    public void ajouter(model.ReferralHit hit) {
        recordHit(hit.getInfluencerId(), hit.getSessionId(), hit.getReferredUserId());
    }

    public java.util.List<model.ReferralHit> recuperer() {
        java.util.List<model.ReferralHit> list = new java.util.ArrayList<>();
        if (!tableExists()) return list;
        try {
            String sql = "SELECT h.*, c.title as col_title, u.email as user_email " +
                         "FROM referral_hit h " +
                         "LEFT JOIN collaboration c ON h.influencer_id = c.id " +
                         "LEFT JOIN user u ON h.referred_user_id = u.id " +
                         "ORDER BY h.id DESC";
            java.sql.ResultSet rs = connection.createStatement().executeQuery(sql);
            while (rs.next()) {
                model.ReferralHit h = new model.ReferralHit();
                h.setId(rs.getInt("id"));
                h.setInfluencerId(rs.getInt("influencer_id"));
                h.setSessionId(rs.getString("session_id"));
                java.sql.Timestamp ts = rs.getTimestamp("visited_at");
                if (ts != null) h.setVisitedAt(ts.toLocalDateTime());
                
                int ref = rs.getInt("referred_user_id");
                if (!rs.wasNull()) h.setReferredUserId(ref);
                
                h.setCollaborationTitle(rs.getString("col_title") != null ? rs.getString("col_title") : "ID: " + h.getInfluencerId());
                h.setReferredUserEmail(rs.getString("user_email") != null ? rs.getString("user_email") : "Anonymous");
                
                list.add(h);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // ── User referral stats ───────────────────────────────────────────

    public int countHitsForInfluencer(int influencerId) {
        return countWhere(influencerId, "referral");
    }

    public int getTotalClicksByUser(int userId) {
        if (!tableExists()) return 0;
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM referral_hit WHERE referred_user_id=? AND session_id='click'");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) { return 0; }
    }

    public String generateReferralCode(int userId) {
        long hash = Math.abs((long) userId * 2654435761L % 99999) + 10000;
        return "EVT-" + String.format("%05d", hash);
    }

    // ── Analytics for Charts ──────────────────────────────────────────

    /** Gets hits per day for a specific collaboration (last 14 days) */
    public Map<java.time.LocalDate, Long> getDailyHitsForCollaboration(int collabId) {
        Map<java.time.LocalDate, Long> stats = new TreeMap<>();
        if (!tableExists()) return stats;
        try {
            String sql = "SELECT DATE(visited_at) as hit_date, COUNT(*) as hit_count " +
                         "FROM referral_hit WHERE influencer_id=? " +
                         "GROUP BY DATE(visited_at) ORDER BY hit_date ASC";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, collabId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                stats.put(rs.getDate("hit_date").toLocalDate(), rs.getLong("hit_count"));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return stats;
    }

    /** Gets clicks vs impressions for a specific collaboration */
    public Map<String, Long> getHitDistribution(int collabId) {
        Map<String, Long> dist = new HashMap<>();
        if (!tableExists()) return dist;
        try {
            String sql = "SELECT session_id, COUNT(*) as c FROM referral_hit WHERE influencer_id=? GROUP BY session_id";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, collabId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                dist.put(rs.getString("session_id"), rs.getLong("c"));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return dist;
    }
}

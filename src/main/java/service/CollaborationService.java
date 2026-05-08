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
            System.out.println("[EVENTO] collaboration table columns detected: " + tableColumns);
        }
        return tableColumns;
    }

    private boolean has(String col) throws SQLException { return columns().contains(col); }

    /** Functional binder for a PreparedStatement parameter slot. */
    @FunctionalInterface
    private interface Binder { void bind(PreparedStatement ps, int idx) throws SQLException; }

    /**
     * Builds an INSERT or UPDATE for the {@code collaboration} table that
     * includes every column the live DB actually has, and skips the rest.
     * The table schema varies between teammates' machines (some have
     * {@code width/height/price/referral_code/view_count/created_at},
     * others don't), so any column outside the always-present minimum set
     * is conditional on {@link #has(String)}.
     *
     * Always-present minimum: {@code partner_id, title, type, file_name,
     * link_url, position, status, start_date, end_date}.
     */
    @Override
    public void ajouter(Collaboration c) throws SQLException {
        java.util.LinkedHashMap<String, Binder> fields = collectWritableFields(c);

        String cols = String.join(", ", fields.keySet());
        String qs   = String.join(", ", java.util.Collections.nCopies(fields.size(), "?"));
        String sql  = "INSERT INTO collaboration (" + cols + ") VALUES (" + qs + ")";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            for (Binder b : fields.values()) b.bind(ps, i++);
            ps.executeUpdate();
        }
    }

    @Override
    public void modifier(Collaboration c) throws SQLException {
        java.util.LinkedHashMap<String, Binder> fields = collectWritableFields(c);

        StringBuilder set = new StringBuilder();
        for (String k : fields.keySet()) {
            if (set.length() > 0) set.append(", ");
            set.append(k).append(" = ?");
        }
        String sql = "UPDATE collaboration SET " + set + " WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            for (Binder b : fields.values()) b.bind(ps, i++);
            ps.setInt(i, c.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Returns an ordered map of {column name → binder} containing every
     * column we know how to write that ALSO exists in the live DB.
     */
    private java.util.LinkedHashMap<String, Binder> collectWritableFields(Collaboration c) throws SQLException {
        java.util.LinkedHashMap<String, Binder> f = new java.util.LinkedHashMap<>();

        // Required-ish core: every schema we've seen has these.
        f.put("partner_id", (ps, i) -> ps.setInt(i, c.getPartnerId()));
        f.put("title",      (ps, i) -> ps.setString(i, c.getTitle()));
        f.put("type",       (ps, i) -> ps.setString(i, c.getType()));
        f.put("file_name",  (ps, i) -> ps.setString(i, c.getFileName()));
        f.put("link_url",   (ps, i) -> ps.setString(i, c.getLinkUrl()));
        f.put("position",   (ps, i) -> ps.setString(i, c.getPosition()));
        f.put("status",     (ps, i) -> ps.setString(i, c.getStatus()));
        f.put("start_date", (ps, i) -> ps.setDate(i,
                c.getStartDate() != null ? Date.valueOf(c.getStartDate()) : null));
        f.put("end_date",   (ps, i) -> ps.setDate(i,
                c.getEndDate()   != null ? Date.valueOf(c.getEndDate())   : null));

        // Optional / version-dependent columns — only if the table has them.
        if (has("created_at")) f.put("created_at",
                (ps, i) -> ps.setTimestamp(i, Timestamp.valueOf(c.getCreatedAt())));
        if (has("view_count")) f.put("view_count",
                (ps, i) -> ps.setInt(i, c.getViewCount()));
        if (has("price")) f.put("price",
                (ps, i) -> { if (c.getPrice() != null) ps.setDouble(i, c.getPrice()); else ps.setNull(i, Types.DOUBLE); });
        if (has("width")) f.put("width",
                (ps, i) -> { if (c.getWidth()  != null) ps.setInt(i, c.getWidth());  else ps.setNull(i, Types.INTEGER); });
        if (has("height")) f.put("height",
                (ps, i) -> { if (c.getHeight() != null) ps.setInt(i, c.getHeight()); else ps.setNull(i, Types.INTEGER); });
        if (has("referral_code")) f.put("referral_code",
                (ps, i) -> ps.setString(i, c.getReferralCode()));

        return f;
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

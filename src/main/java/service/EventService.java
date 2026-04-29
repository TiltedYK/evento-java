package service;

import model.Event;
import utils.MyDatabase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class EventService implements IService<Event> {

    private Connection connection;
    private Set<String> tableColumns = null;

    public EventService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    // ── Column detection (cached, checked once per session) ───────────────
    private Set<String> columns() throws SQLException {
        if (tableColumns == null) {
            tableColumns = new HashSet<>();
            ResultSet rs = connection.getMetaData().getColumns(null, null, "event", null);
            while (rs.next()) tableColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
        }
        return tableColumns;
    }

    private boolean has(String col) throws SQLException {
        return columns().contains(col);
    }

    // ── CRUD ──────────────────────────────────────────────────────────────

    @Override
    public void ajouter(Event e) throws SQLException {
        List<String> cols = new ArrayList<>(Arrays.asList("titre", "date_heure", "capacite", "description", "statut", "venue"));
        if (has("location"))         cols.add("location");
        if (has("latitude"))         cols.add("latitude");
        if (has("longitude"))        cols.add("longitude");
        if (has("genre"))            cols.add("genre");
        if (has("artist_id"))        cols.add("artist_id");
        if (has("image_filename"))   cols.add("image_filename");
        if (has("created_by_id"))    cols.add("created_by_id");

        String placeholders = String.join(",", Collections.nCopies(cols.size(), "?"));
        String sql = "INSERT INTO event (" + String.join(",", cols) + ") VALUES (" + placeholders + ")";
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        int i = 1;
        ps.setString(i++, e.getTitre());
        ps.setTimestamp(i++, e.getDateHeure() != null ? Timestamp.valueOf(e.getDateHeure()) : null);
        ps.setInt(i++, e.getCapacite());
        ps.setString(i++, e.getDescription());
        ps.setString(i++, e.getStatut());
        ps.setString(i++, e.getVenue());
        if (has("location"))       ps.setString(i++, e.getLocation());
        if (has("latitude"))       { if (e.getLatitude()  != null) ps.setDouble(i++, e.getLatitude());  else ps.setNull(i++, Types.DOUBLE);  }
        if (has("longitude"))      { if (e.getLongitude() != null) ps.setDouble(i++, e.getLongitude()); else ps.setNull(i++, Types.DOUBLE);  }
        if (has("genre"))          ps.setString(i++, e.getGenre());
        if (has("artist_id"))      { if (e.getArtistId()  != null) ps.setInt(i++, e.getArtistId());     else ps.setNull(i++, Types.INTEGER); }
        if (has("image_filename")) ps.setString(i++, e.getImageFilename());
        if (has("created_by_id"))  { if (e.getCreatedById() != null) ps.setInt(i++, e.getCreatedById()); else ps.setNull(i++, Types.INTEGER); }
        ps.executeUpdate();

        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) e.setId(keys.getInt(1));
    }

    @Override
    public void modifier(Event e) throws SQLException {
        List<String> setCols = new ArrayList<>(Arrays.asList("titre=?", "date_heure=?", "capacite=?", "description=?", "statut=?", "venue=?"));
        if (has("location"))       setCols.add("location=?");
        if (has("latitude"))       setCols.add("latitude=?");
        if (has("longitude"))      setCols.add("longitude=?");
        if (has("genre"))          setCols.add("genre=?");
        if (has("image_filename")) setCols.add("image_filename=?");

        String sql = "UPDATE event SET " + String.join(",", setCols) + " WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        int i = 1;
        ps.setString(i++, e.getTitre());
        ps.setTimestamp(i++, e.getDateHeure() != null ? Timestamp.valueOf(e.getDateHeure()) : null);
        ps.setInt(i++, e.getCapacite());
        ps.setString(i++, e.getDescription());
        ps.setString(i++, e.getStatut());
        ps.setString(i++, e.getVenue());
        if (has("location"))       ps.setString(i++, e.getLocation());
        if (has("latitude"))       { if (e.getLatitude()  != null) ps.setDouble(i++, e.getLatitude());  else ps.setNull(i++, Types.DOUBLE); }
        if (has("longitude"))      { if (e.getLongitude() != null) ps.setDouble(i++, e.getLongitude()); else ps.setNull(i++, Types.DOUBLE); }
        if (has("genre"))          ps.setString(i++, e.getGenre());
        if (has("image_filename")) ps.setString(i++, e.getImageFilename());
        ps.setInt(i, e.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("DELETE FROM event WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Event> recuperer() throws SQLException {
        ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM event");
        Set<String> c = columns();
        List<Event> list = new ArrayList<>();
        while (rs.next()) {
            Event e = new Event();
            e.setId(rs.getInt("id"));
            e.setTitre(rs.getString("titre"));
            Timestamp ts = rs.getTimestamp("date_heure");
            if (ts != null) e.setDateHeure(ts.toLocalDateTime());
            e.setCapacite(rs.getInt("capacite"));
            e.setDescription(rs.getString("description"));
            e.setStatut(rs.getString("statut"));
            e.setVenue(rs.getString("venue"));
            if (c.contains("location"))       e.setLocation(rs.getString("location"));
            if (c.contains("latitude"))       e.setLatitude(rs.getObject("latitude")   != null ? rs.getDouble("latitude")   : null);
            if (c.contains("longitude"))      e.setLongitude(rs.getObject("longitude") != null ? rs.getDouble("longitude")  : null);
            if (c.contains("genre"))          e.setGenre(rs.getString("genre"));
            if (c.contains("artist_id"))      e.setArtistId(rs.getObject("artist_id")  != null ? rs.getInt("artist_id")     : null);
            if (c.contains("image_filename")) e.setImageFilename(rs.getString("image_filename"));
            if (c.contains("created_by_id")) e.setCreatedById(rs.getObject("created_by_id") != null ? rs.getInt("created_by_id") : null);
            list.add(e);
        }
        return list;
    }

    // ── Advanced queries (stream-based) ───────────────────────────────────

    public List<Event> rechercherParTitre(String keyword) throws SQLException {
        return recuperer().stream()
                .filter(e -> e.getTitre() != null && e.getTitre().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Event> rechercherParStatut(String statut) throws SQLException {
        return recuperer().stream()
                .filter(e -> e.getStatut() != null && e.getStatut().equalsIgnoreCase(statut))
                .collect(Collectors.toList());
    }

    public List<Event> rechercherParVenue(String keyword) throws SQLException {
        return recuperer().stream()
                .filter(e -> e.getVenue() != null && e.getVenue().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Event> rechercherParGenre(String genre) throws SQLException {
        return recuperer().stream()
                .filter(e -> e.getGenre() != null && e.getGenre().equalsIgnoreCase(genre))
                .collect(Collectors.toList());
    }

    public List<Event> getUpcoming() throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        return recuperer().stream()
                .filter(e -> e.getDateHeure() != null && e.getDateHeure().isAfter(now))
                .filter(e -> !"cancelled".equalsIgnoreCase(e.getStatut()))
                .sorted(Comparator.comparing(Event::getDateHeure))
                .collect(Collectors.toList());
    }

    public List<Event> sortedByDate(boolean asc) throws SQLException {
        Comparator<Event> cmp = Comparator.comparing(
                Event::getDateHeure, Comparator.nullsLast(Comparator.naturalOrder()));
        return recuperer().stream()
                .sorted(asc ? cmp : cmp.reversed())
                .collect(Collectors.toList());
    }

    public List<Event> sortedByTitle() throws SQLException {
        return recuperer().stream()
                .sorted(Comparator.comparing(e -> e.getTitre() != null ? e.getTitre().toLowerCase() : ""))
                .collect(Collectors.toList());
    }

    public List<Event> sortedByCapacity(boolean asc) throws SQLException {
        Comparator<Event> cmp = Comparator.comparingInt(Event::getCapacite);
        return recuperer().stream()
                .sorted(asc ? cmp : cmp.reversed())
                .collect(Collectors.toList());
    }

    public Event dupliquer(int id) throws SQLException {
        Event orig = recuperer().stream().filter(e -> e.getId() == id).findFirst().orElse(null);
        if (orig == null) return null;
        Event copy = new Event();
        copy.setTitre(orig.getTitre() + " (Copy)");
        copy.setDateHeure(orig.getDateHeure());
        copy.setCapacite(orig.getCapacite());
        copy.setDescription(orig.getDescription());
        copy.setStatut("draft");
        copy.setVenue(orig.getVenue());
        copy.setLocation(orig.getLocation());
        copy.setGenre(orig.getGenre());
        ajouter(copy);
        return copy;
    }

    public void exportToCSV(List<Event> events, File file) throws IOException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("ID,Title,Date,Venue,Genre,Status,Capacity\n");
            for (Event e : events) {
                fw.write(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d\n",
                        e.getId(),
                        safe(e.getTitre()),
                        e.getDateHeure() != null ? e.getDateHeure().format(fmt) : "",
                        safe(e.getVenue()),
                        safe(e.getGenre()),
                        safe(e.getStatut()),
                        e.getCapacite()));
            }
        }
    }

    private String safe(String s) {
        return s != null ? s.replace("\"", "\"\"") : "";
    }
}

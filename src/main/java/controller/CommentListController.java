package controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DefaultStringConverter;
import model.Comment;
import service.CommentService;
import util.Router;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class CommentListController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> stateFilter;
    @FXML private TableView<Comment> table;
    @FXML private TableColumn<Comment, String> colContent;
    @FXML private TableColumn<Comment, String> colCreated;
    @FXML private TableColumn<Comment, String> colState;
    @FXML private TableColumn<Comment, Void> colActions;

    private final CommentService service = new CommentService();
    private final ObservableList<Comment> data = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static Comment pendingEdit;

    @FXML
    public void initialize() {
        stateFilter.getItems().addAll("all", "active", "deleted");
        stateFilter.setValue("all");

        table.setEditable(true);

        colContent.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getContent() == null ? "" : c.getValue().getContent()));
        colCreated.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() != null ? c.getValue().getCreatedAt().format(FMT) : ""));
        colState.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDeletedAt() != null ? "deleted" : "active"));

        colContent.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colContent.setOnEditCommit(e -> {
            Comment c = e.getRowValue(); c.setContent(e.getNewValue());
            try { service.modifier(c); } catch (Exception ex) { error("Save failed: " + ex.getMessage()); }
        });

        setupStatePill();
        setupActions();

        searchField.textProperty().addListener((o, a, b) -> applyFilters());
        stateFilter.valueProperty().addListener((o, a, b) -> applyFilters());

        table.setItems(data);
        refresh();
    }

    private String preview(String s) {
        if (s == null) return "";
        String clean = s.replaceAll("\\s+", " ").trim();
        return clean.length() > 80 ? clean.substring(0, 80) + "…" : clean;
    }

    private void setupStatePill() {
        colState.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); }
                else {
                    Label pill = new Label(item);
                    pill.getStyleClass().add("status-pill");
                    pill.getStyleClass().add(item.equals("active") ? "status-confirmed" : "status-cancelled");
                    setGraphic(pill); setText(null);
                }
            }
        });
    }

    private void setupActions() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("Delete");
            {
                btnDelete.getStyleClass().addAll("button", "btn-danger", "btn-action");
                btnDelete.setOnAction(e -> onDelete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
            }
        });
    }

    private void refresh() {
        try { data.setAll(service.recuperer()); applyFilters(); }
        catch (Exception e) { error("Failed to load comments: " + e.getMessage()); }
    }

    private void applyFilters() {
        try {
            List<Comment> all = service.recuperer();
            String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            String st = stateFilter.getValue();
            List<Comment> filtered = all.stream()
                    .filter(c -> q.isEmpty()
                            || (c.getContent() != null && c.getContent().toLowerCase().contains(q)))
                    .filter(c -> st == null || st.equals("all")
                            || (st.equals("active") && c.getDeletedAt() == null)
                            || (st.equals("deleted") && c.getDeletedAt() != null))
                    .toList();
            data.setAll(filtered);
        } catch (Exception e) { error("Filter failed: " + e.getMessage()); }
    }

    @FXML public void onReset() { searchField.clear(); stateFilter.setValue("all"); }
    @FXML public void onAdd() {
        pendingEdit = null;
        Router.navigate("/fxml/CommentForm.fxml");
    }

    private void onDelete(Comment c) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm deletion");
        a.setHeaderText("Delete this comment?");
        a.setContentText("\"" + preview(c.getContent()) + "\"");
        styleAlert(a);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try { service.supprimer(c.getId()); refresh(); }
                catch (Exception e) { error("Delete failed: " + e.getMessage()); }
            }
        });
    }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error"); a.setHeaderText(null); a.setContentText(msg);
        styleAlert(a); a.showAndWait();
    }

    private void styleAlert(Alert a) {
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
    }
}

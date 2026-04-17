package controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import model.Comment;
import service.CommentService;
import util.Router;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class CommentListController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> stateFilter;
    @FXML private TableView<Comment> table;
    @FXML private TableColumn<Comment, Number> colId;
    @FXML private TableColumn<Comment, Number> colPost;
    @FXML private TableColumn<Comment, Number> colAuthor;
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

        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colPost.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getPostId()));
        colAuthor.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getAuthorId()));
        colContent.setCellValueFactory(c -> new SimpleStringProperty(preview(c.getValue().getContent())));
        colCreated.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() != null ? c.getValue().getCreatedAt().format(FMT) : ""));
        colState.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDeletedAt() != null ? "deleted" : "active"));

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
            private final Button btnView = new Button("View");
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox box = new HBox(6, btnView, btnEdit, btnDelete);
            {
                btnView.getStyleClass().addAll("button", "btn-ghost", "btn-action");
                btnEdit.getStyleClass().addAll("button", "btn-primary", "btn-action");
                btnDelete.getStyleClass().addAll("button", "btn-danger", "btn-action");
                btnView.setOnAction(e -> onView(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(e -> openForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> onDelete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
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
                            || (c.getContent() != null && c.getContent().toLowerCase().contains(q))
                            || String.valueOf(c.getPostId()).equals(q)
                            || String.valueOf(c.getAuthorId()).equals(q))
                    .filter(c -> st == null || st.equals("all")
                            || (st.equals("active") && c.getDeletedAt() == null)
                            || (st.equals("deleted") && c.getDeletedAt() != null))
                    .toList();
            data.setAll(filtered);
        } catch (Exception e) { error("Filter failed: " + e.getMessage()); }
    }

    @FXML public void onReset() { searchField.clear(); stateFilter.setValue("all"); }
    @FXML public void onAdd() { openForm(null); }

    private void onView(Comment c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Comment");
        a.setHeaderText("Comment #" + c.getId() + " — post " + c.getPostId());
        a.setContentText(c.getContent() == null ? "(empty)" : c.getContent());
        a.getDialogPane().setPrefWidth(520);
        styleAlert(a); a.showAndWait();
    }

    private void onDelete(Comment c) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm deletion");
        a.setHeaderText("Delete comment #" + c.getId() + "?");
        a.setContentText("This action cannot be undone.");
        styleAlert(a);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try { service.supprimer(c.getId()); refresh(); }
                catch (Exception e) { error("Delete failed: " + e.getMessage()); }
            }
        });
    }

    private void openForm(Comment c) {
        pendingEdit = c;
        Router.navigate("/fxml/CommentForm.fxml");
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

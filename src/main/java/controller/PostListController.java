package controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import model.Post;
import service.PostService;
import util.Router;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class PostListController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> stateFilter;
    @FXML private TableView<Post> table;
    @FXML private TableColumn<Post, Number> colId;
    @FXML private TableColumn<Post, String> colTitle;
    @FXML private TableColumn<Post, String> colSlug;
    @FXML private TableColumn<Post, Number> colAuthor;
    @FXML private TableColumn<Post, String> colCreated;
    @FXML private TableColumn<Post, String> colState;
    @FXML private TableColumn<Post, Void> colActions;

    private final PostService service = new PostService();
    private final ObservableList<Post> data = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static Post pendingEdit;

    @FXML
    public void initialize() {
        stateFilter.getItems().addAll("all", "active", "deleted");
        stateFilter.setValue("all");

        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colTitle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        colSlug.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSlug()));
        colAuthor.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getAuthorId()));
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
        catch (Exception e) { error("Failed to load posts: " + e.getMessage()); }
    }

    private void applyFilters() {
        try {
            List<Post> all = service.recuperer();
            String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            String st = stateFilter.getValue();

            List<Post> filtered = all.stream()
                    .filter(p -> q.isEmpty()
                            || (p.getTitle() != null && p.getTitle().toLowerCase().contains(q))
                            || (p.getContent() != null && p.getContent().toLowerCase().contains(q)))
                    .filter(p -> st == null || st.equals("all")
                            || (st.equals("active") && p.getDeletedAt() == null)
                            || (st.equals("deleted") && p.getDeletedAt() != null))
                    .toList();
            data.setAll(filtered);
        } catch (Exception e) { error("Filter failed: " + e.getMessage()); }
    }

    @FXML public void onReset() { searchField.clear(); stateFilter.setValue("all"); }
    @FXML public void onAdd() { openForm(null); }

    private void onView(Post p) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Post preview");
        a.setHeaderText(p.getTitle());
        String body = p.getContent() == null ? "" : p.getContent();
        if (body.length() > 1500) body = body.substring(0, 1500) + "\n\n… (truncated)";
        a.setContentText(body);
        a.getDialogPane().setPrefWidth(560);
        styleAlert(a); a.showAndWait();
    }

    private void onDelete(Post p) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm deletion");
        a.setHeaderText("Delete post #" + p.getId() + "?");
        a.setContentText("\"" + p.getTitle() + "\"\nThis will also hide related comments.");
        styleAlert(a);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try { service.supprimer(p.getId()); refresh(); }
                catch (Exception e) { error("Delete failed: " + e.getMessage()); }
            }
        });
    }

    private void openForm(Post p) {
        pendingEdit = p;
        Router.navigate("/fxml/PostForm.fxml");
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

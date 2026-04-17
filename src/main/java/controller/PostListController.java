package controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Post;
import service.PostService;

import java.time.LocalDateTime;
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

        setupStatePillColumn();
        setupActionsColumn();

        searchField.textProperty().addListener((o, a, b) -> applyFilters());
        stateFilter.valueProperty().addListener((o, a, b) -> applyFilters());

        table.setItems(data);
        refresh();
    }

    private void setupStatePillColumn() {
        colState.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); }
                else {
                    Label pill = new Label(item);
                    pill.getStyleClass().add("status-pill");
                    pill.getStyleClass().add(item.equals("active") ? "status-confirmed" : "status-cancelled");
                    setGraphic(pill);
                    setText(null);
                }
            }
        });
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView = new Button("View");
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox box = new HBox(6, btnView, btnEdit, btnDelete);
            {
                btnView.getStyleClass().addAll("button", "btn-ghost");
                btnEdit.getStyleClass().addAll("button", "btn-primary");
                btnDelete.getStyleClass().addAll("button", "btn-danger");
                btnView.setStyle("-fx-font-size: 11px; -fx-padding: 5 10;");
                btnEdit.setStyle("-fx-font-size: 11px; -fx-padding: 5 10;");
                btnDelete.setStyle("-fx-font-size: 11px; -fx-padding: 5 10;");
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
        try {
            data.setAll(service.recuperer());
            applyFilters();
        } catch (Exception e) { showError("Failed to load posts", e.getMessage()); }
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
        } catch (Exception e) { showError("Filter failed", e.getMessage()); }
    }

    @FXML public void onReset() {
        searchField.clear();
        stateFilter.setValue("all");
    }

    @FXML public void onAdd() { openForm(null); }

    private void onView(Post p) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Post preview");
        a.setHeaderText(p.getTitle());
        String body = (p.getContent() == null ? "" : p.getContent());
        if (body.length() > 1500) body = body.substring(0, 1500) + "\n\n… (truncated)";
        a.setContentText(body);
        a.getDialogPane().setPrefWidth(560);
        styleAlert(a);
        a.showAndWait();
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
                catch (Exception e) { showError("Delete failed", e.getMessage()); }
            }
        });
    }

    private void openForm(Post p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PostForm.fxml"));
            Parent root = loader.load();
            PostFormController ctrl = loader.getController();
            ctrl.setPost(p);

            Stage st = new Stage();
            st.initModality(Modality.APPLICATION_MODAL);
            st.setTitle(p == null ? "New Post" : "Edit Post");
            Scene sc = new Scene(root);
            sc.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            st.setScene(sc);
            st.showAndWait();
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not open form", e.getMessage());
        }
    }

    private void showError(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(header);
        a.setContentText(msg);
        styleAlert(a);
        a.showAndWait();
    }

    private void styleAlert(Alert a) {
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
    }
}

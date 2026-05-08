package controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DefaultStringConverter;
import model.Post;
import service.PostService;
import util.Router;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class PostListController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> stateFilter;
    @FXML private TableView<Post> table;
    @FXML private TableColumn<Post, String> colTitle;
    @FXML private TableColumn<Post, String> colSlug;
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

        table.setEditable(true);

        colTitle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        colSlug.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSlug()));
        colCreated.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() != null ? c.getValue().getCreatedAt().format(FMT) : ""));
        colState.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDeletedAt() != null ? "deleted" : "active"));

        colTitle.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colTitle.setOnEditCommit(e -> {
            Post p = e.getRowValue(); p.setTitle(e.getNewValue());
            try { service.modifier(p); } catch (Exception ex) { error("Save failed: " + ex.getMessage()); }
        });

        colSlug.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colSlug.setOnEditCommit(e -> {
            Post p = e.getRowValue(); p.setSlug(e.getNewValue());
            try { service.modifier(p); } catch (Exception ex) { error("Save failed: " + ex.getMessage()); }
        });

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
    @FXML public void onAdd() {
        pendingEdit = null;
        Router.navigate("/fxml/PostForm.fxml");
    }

    private void onDelete(Post p) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm deletion");
        a.setHeaderText("Delete post \"" + p.getTitle() + "\"?");
        a.setContentText("This will also hide related comments.");
        styleAlert(a);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try { service.supprimer(p.getId()); refresh(); }
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

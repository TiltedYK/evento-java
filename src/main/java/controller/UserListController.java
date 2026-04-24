package controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.util.converter.DefaultStringConverter;
import model.User;
import service.UserService;
import util.Router;

import java.util.List;

public class UserListController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> stateFilter;
    @FXML private TableView<User> table;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colPrenom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colTel;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colState;
    @FXML private TableColumn<User, Void> colActions;

    private final UserService service = new UserService();
    private final ObservableList<User> data = FXCollections.observableArrayList();

    public static User pendingEdit;

    @FXML
    public void initialize() {
        roleFilter.getItems().addAll("all", "ROLE_USER", "ROLE_ADMIN", "ROLE_ARTIST");
        roleFilter.setValue("all");
        stateFilter.getItems().addAll("all", "active", "banned", "deleted");
        stateFilter.setValue("all");

        table.setEditable(true);

        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colPrenom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrenom()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colTel.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumTelephone()));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(shortRole(c.getValue().getRoles())));
        colState.setCellValueFactory(c -> new SimpleStringProperty(userState(c.getValue())));

        colNom.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colNom.setOnEditCommit(e -> {
            User u = e.getRowValue(); u.setNom(e.getNewValue());
            try { service.modifier(u); } catch (Exception ex) { error("Save failed: " + ex.getMessage()); }
        });

        colPrenom.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colPrenom.setOnEditCommit(e -> {
            User u = e.getRowValue(); u.setPrenom(e.getNewValue());
            try { service.modifier(u); } catch (Exception ex) { error("Save failed: " + ex.getMessage()); }
        });

        colEmail.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colEmail.setOnEditCommit(e -> {
            User u = e.getRowValue(); u.setEmail(e.getNewValue());
            try { service.modifier(u); } catch (Exception ex) { error("Save failed: " + ex.getMessage()); }
        });

        colTel.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colTel.setOnEditCommit(e -> {
            User u = e.getRowValue(); u.setNumTelephone(e.getNewValue());
            try { service.modifier(u); } catch (Exception ex) { error("Save failed: " + ex.getMessage()); }
        });

        setupStatePill();
        setupActions();

        searchField.textProperty().addListener((o, a, b) -> applyFilters());
        roleFilter.valueProperty().addListener((o, a, b) -> applyFilters());
        stateFilter.valueProperty().addListener((o, a, b) -> applyFilters());

        table.setItems(data);
        refresh();
    }

    private String shortRole(String roles) {
        if (roles == null) return "";
        if (roles.contains("ADMIN")) return "Admin";
        if (roles.contains("ARTIST")) return "Artist";
        return "User";
    }

    private String userState(User u) {
        if (u.getDeleted() == 1) return "deleted";
        if (u.getBanned() == 1) return "banned";
        return "active";
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
                    switch (item) {
                        case "active"  -> pill.getStyleClass().add("status-confirmed");
                        case "banned"  -> pill.getStyleClass().add("status-cancelled");
                        case "deleted" -> pill.getStyleClass().add("status-pending");
                        default        -> pill.getStyleClass().add("status-draft");
                    }
                    setGraphic(pill); setText(null);
                }
            }
        });
    }

    private void setupActions() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnBan = new Button("Ban");
            private final Button btnDelete = new Button("Delete");
            private final HBox box = new HBox(6, btnBan, btnDelete);
            {
                btnBan.getStyleClass().addAll("button", "btn-ghost", "btn-action");
                btnDelete.getStyleClass().addAll("button", "btn-danger", "btn-action");
                btnBan.setOnAction(e -> onToggleBan(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> onDelete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                User u = getTableView().getItems().get(getIndex());
                btnBan.setText(u.getBanned() == 1 ? "Unban" : "Ban");
                setGraphic(box);
            }
        });
    }

    private void refresh() {
        try { data.setAll(service.recuperer()); applyFilters(); }
        catch (Exception e) { error("Failed to load users: " + e.getMessage()); }
    }

    private void applyFilters() {
        try {
            List<User> all = service.recuperer();
            String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            String role = roleFilter.getValue();
            String state = stateFilter.getValue();
            List<User> filtered = all.stream()
                    .filter(u -> q.isEmpty()
                            || (u.getNom() != null && u.getNom().toLowerCase().contains(q))
                            || (u.getPrenom() != null && u.getPrenom().toLowerCase().contains(q))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q))
                            || (u.getNumTelephone() != null && u.getNumTelephone().contains(q)))
                    .filter(u -> role == null || role.equals("all")
                            || (u.getRoles() != null && u.getRoles().contains(role)))
                    .filter(u -> state == null || state.equals("all") || userState(u).equals(state))
                    .toList();
            data.setAll(filtered);
        } catch (Exception e) { error("Filter failed: " + e.getMessage()); }
    }

    @FXML public void onReset() {
        searchField.clear(); roleFilter.setValue("all"); stateFilter.setValue("all");
    }

    @FXML public void onAdd() {
        pendingEdit = null;
        Router.navigate("/fxml/UserForm.fxml");
    }

    private void onDelete(User u) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm deletion");
        a.setHeaderText("Delete user \"" + u.getPrenom() + " " + u.getNom() + "\"?");
        a.setContentText("This action cannot be undone.");
        styleAlert(a);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try { service.supprimer(u.getId()); refresh(); }
                catch (Exception e) { error("Delete failed: " + e.getMessage()); }
            }
        });
    }

    private void onToggleBan(User u) {
        try {
            u.setBanned(u.getBanned() == 1 ? 0 : 1);
            service.modifier(u);
            refresh();
        } catch (Exception e) { error("Update failed: " + e.getMessage()); }
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

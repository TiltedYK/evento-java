package controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DefaultStringConverter;
import model.Collaboration;
import model.PartnershipRequest;
import service.CollaborationService;
import service.PartnershipRequestService;
import service.ReferralHitService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class PartnershipRequestController implements Initializable {

    @FXML private TableView<PartnershipRequest> tableRequests;
    @FXML private TableColumn<PartnershipRequest, String> colCompany;
    @FXML private TableColumn<PartnershipRequest, String> colContact;
    @FXML private TableColumn<PartnershipRequest, String> colEmail;
    @FXML private TableColumn<PartnershipRequest, String> colStatus;
    @FXML private TableColumn<PartnershipRequest, LocalDateTime> colDate;

    @FXML private Label lblStatTotal;
    @FXML private Label lblStatPending;
    @FXML private Label lblStatToday;

    @FXML private TextField fldCompany;
    @FXML private TextField fldContact;
    @FXML private TextField fldEmail;
    @FXML private TextField fldPhone;
    @FXML private TextArea fldMessage;

    private final PartnershipRequestService requestService = new PartnershipRequestService();
    private final CollaborationService collaborationService = new CollaborationService();
    private final ReferralHitService referralHitService = new ReferralHitService();
    private final ObservableList<PartnershipRequest> requestsList = FXCollections.observableArrayList();
    private PartnershipRequest selectedRequest = null;
    private boolean syncingSidebar = false;

    private static final DateTimeFormatter DT_DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        tableRequests.setEditable(true);

        colCompany.setCellValueFactory(c -> new SimpleStringProperty(str(c.getValue().getCompanyName())));
        colCompany.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colCompany.setOnEditCommit(e -> {
            PartnershipRequest r = e.getRowValue();
            r.setCompanyName(e.getNewValue());
            persist(r);
        });

        colContact.setCellValueFactory(c -> new SimpleStringProperty(str(c.getValue().getContactName())));
        colContact.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colContact.setOnEditCommit(e -> {
            PartnershipRequest r = e.getRowValue();
            r.setContactName(e.getNewValue());
            persist(r);
        });

        colEmail.setCellValueFactory(c -> new SimpleStringProperty(str(c.getValue().getEmail())));
        colEmail.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colEmail.setOnEditCommit(e -> {
            PartnershipRequest r = e.getRowValue();
            r.setEmail(e.getNewValue());
            persist(r);
        });

        colStatus.setCellValueFactory(c -> new SimpleStringProperty(str(c.getValue().getStatus())));
        colStatus.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colStatus.setOnEditCommit(e -> {
            PartnershipRequest r = e.getRowValue();
            r.setStatus(e.getNewValue());
            persist(r);
        });

        colDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCreatedAt()));
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(DT_DISPLAY));
                }
            }
        });

        for (TextField tf : List.of(fldCompany, fldContact, fldEmail, fldPhone)) {
            tf.focusedProperty().addListener((o, was, now) -> {
                if (Boolean.TRUE.equals(was) && Boolean.FALSE.equals(now)) {
                    persistSidebarIfNeeded();
                }
            });
        }
        fldMessage.focusedProperty().addListener((o, was, now) -> {
            if (Boolean.TRUE.equals(was) && Boolean.FALSE.equals(now)) {
                persistSidebarIfNeeded();
            }
        });

        loadData();

        tableRequests.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                fillForm(newSel);
            }
        });
    }

    private void persist(PartnershipRequest r) {
        try {
            requestService.modifier(r);
            tableRequests.refresh();
            refreshStats(requestsList);
            if (selectedRequest != null && selectedRequest.getId() == r.getId()) {
                fillForm(r);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", "Impossible d'enregistrer.");
        }
    }

    private void persistSidebarIfNeeded() {
        if (syncingSidebar || selectedRequest == null) return;
        try {
            PartnershipRequest r = selectedRequest;
            r.setCompanyName(fldCompany.getText() != null ? fldCompany.getText().trim() : "");
            r.setContactName(fldContact.getText() != null ? fldContact.getText().trim() : "");
            r.setEmail(fldEmail.getText() != null ? fldEmail.getText().trim() : "");
            r.setPhone(fldPhone.getText() != null ? fldPhone.getText().trim() : "");
            r.setMessage(fldMessage.getText() != null ? fldMessage.getText().trim() : "");
            requestService.modifier(r);
            tableRequests.refresh();
            refreshStats(requestsList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", "Impossible d'enregistrer.");
        }
    }

    private void loadData() {
        try {
            List<PartnershipRequest> list = requestService.recuperer();
            requestsList.setAll(list);
            tableRequests.setItems(requestsList);
            refreshStats(list);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur BD", "Impossible de charger les requêtes.");
        }
    }

    private void refreshStats(List<PartnershipRequest> list) {
        long total = list.size();
        long pending = list.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())).count();
        long today = list.stream().filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().equals(LocalDate.now())).count();
        if (lblStatTotal != null) lblStatTotal.setText(String.valueOf(total));
        if (lblStatPending != null) lblStatPending.setText(String.valueOf(pending));
        if (lblStatToday != null) lblStatToday.setText(String.valueOf(today));
    }

    private void fillForm(PartnershipRequest request) {
        syncingSidebar = true;
        try {
            selectedRequest = request;
            fldCompany.setText(request.getCompanyName() == null ? "" : request.getCompanyName());
            fldContact.setText(request.getContactName() == null ? "" : request.getContactName());
            fldEmail.setText(request.getEmail() == null ? "" : request.getEmail());
            fldPhone.setText(request.getPhone() == null ? "" : request.getPhone());
            fldMessage.setText(request.getMessage() == null ? "" : request.getMessage());
        } finally {
            syncingSidebar = false;
        }
    }

    private void clearSelection() {
        selectedRequest = null;
        fldCompany.clear();
        fldContact.clear();
        fldEmail.clear();
        fldPhone.clear();
        fldMessage.clear();
        tableRequests.getSelectionModel().clearSelection();
    }

    @FXML
    void handleDelete(ActionEvent event) {
        if (selectedRequest == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner une requête.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer définitivement cette demande ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();

        if (confirm.getResult() == ButtonType.YES) {
            try {
                requestService.supprimer(selectedRequest.getId());
                loadData();
                clearSelection();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Demande supprimée.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de suppression.");
            }
        }
    }

    @FXML
    void handleAccept(ActionEvent event) {
        if (selectedRequest == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Sélectionnez une requête en attente.");
            return;
        }

        if (!"PENDING".equalsIgnoreCase(selectedRequest.getStatus())) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "Cette requête a déjà été traitée.");
            return;
        }

        int partnerId = selectedRequest.getUserId() != null ? selectedRequest.getUserId() : 1;
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusMonths(1);

        try {
            String referralCode = referralHitService.generateReferralCode(partnerId);
            double estimatedPrice = collaborationService.calculateEstimatedPrice("image", "top", start, end);

            Collaboration newCollab = new Collaboration(
                    partnerId,
                    "Collab : " + selectedRequest.getCompanyName(),
                    "image",
                    "",
                    referralCode,
                    "top",
                    start,
                    end);
            newCollab.setStatus("PENDING");
            newCollab.setPrice(estimatedPrice);
            newCollab.setReferralCode(referralCode);

            collaborationService.ajouter(newCollab);

            selectedRequest.setStatus("ACCEPTED");
            requestService.modifier(selectedRequest);

            try {
                service.UserService userService = new service.UserService();
                model.User u = userService.recupererParId(partnerId);
                if (u != null) {
                    String roles = u.getRoles();
                    if (roles == null) roles = "[\"ROLE_USER\"]";
                    if (!roles.contains("ROLE_PARTNER")) {
                        if (roles.equals("[]")) {
                            roles = "[\"ROLE_PARTNER\"]";
                        } else {
                            roles = roles.replace("]", ",\"ROLE_PARTNER\"]");
                        }
                        u.setRoles(roles);
                        userService.modifier(u);
                    }
                }
            } catch (Exception ex) {
                System.err.println("Could not update user role: " + ex.getMessage());
            }

            loadData();
            clearSelection();

            String successMsg = String.format(
                    "La demande a été acceptée !\n\n- Collaboration ajoutée.\n"
                            + "- Code Partenaire : %s\n- Prix Estimé : %.2f TND",
                    referralCode, estimatedPrice);
            showAlert(Alert.AlertType.INFORMATION, "Collaboration Créée", successMsg);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur Base de données",
                    "Erreur lors de l'acceptation : " + e.getMessage());
        }
    }

    @FXML
    void handleReject(ActionEvent event) {
        if (selectedRequest == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Sélectionnez une requête.");
            return;
        }

        if (!"PENDING".equalsIgnoreCase(selectedRequest.getStatus())) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "Cette requête a déjà été traitée.");
            return;
        }

        try {
            selectedRequest.setStatus("REJECTED");
            requestService.modifier(selectedRequest);
            loadData();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Demande refusée.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du refus.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static String str(String s) {
        return s == null ? "" : s;
    }
}

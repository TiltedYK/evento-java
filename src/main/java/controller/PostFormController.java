package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Post;
import service.PostService;

import java.time.LocalDateTime;

public class PostFormController {

    @FXML private Label formTitle;
    @FXML private TextField titleField;
    @FXML private TextField authorIdField;
    @FXML private TextField slugField;
    @FXML private TextField imageField;
    @FXML private TextArea contentArea;
    @FXML private Button saveButton;

    private final PostService service = new PostService();
    private Post editing;

    @FXML
    public void initialize() {
        // Live slug generation from title
        titleField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) { slugField.setText(""); return; }
            slugField.setText(generateSlug(newV));
        });
    }

    public void setPost(Post p) {
        this.editing = p;
        if (p == null) {
            formTitle.setText("New Post");
            saveButton.setText("Publish post");
            return;
        }
        formTitle.setText("Edit Post #" + p.getId());
        saveButton.setText("Save changes");

        titleField.setText(p.getTitle());
        authorIdField.setText(String.valueOf(p.getAuthorId()));
        slugField.setText(p.getSlug());
        imageField.setText(p.getImage());
        contentArea.setText(p.getContent());
    }

    @FXML
    public void onSave() {
        String title = safe(titleField.getText());
        String content = safe(contentArea.getText());

        if (title.isEmpty())   { showError("Title is required."); return; }
        if (title.length() < 3){ showError("Title must be at least 3 characters."); return; }
        if (content.isEmpty()) { showError("Content is required."); return; }

        int authorId;
        try {
            authorId = Integer.parseInt(authorIdField.getText().trim());
            if (authorId <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            showError("Author ID must be a positive number.");
            return;
        }

        Post target = editing != null ? editing : new Post();
        target.setTitle(title);
        target.setAuthorId(authorId);
        target.setSlug(generateSlug(title));
        target.setContent(content);
        target.setImage(safe(imageField.getText()));

        if (editing == null) {
            target.setCreatedAt(LocalDateTime.now());
        } else {
            target.setUpdatedAt(LocalDateTime.now());
        }

        try {
            if (editing == null) service.ajouter(target);
            else service.modifier(target);
            close();
        } catch (Exception e) {
            showError("Save failed: " + e.getMessage());
        }
    }

    @FXML public void onCancel() { close(); }

    private void close() { ((Stage) titleField.getScene().getWindow()).close(); }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private String generateSlug(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Validation");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        a.showAndWait();
    }
}

package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Post;
import service.PostService;
import util.Router;

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
        titleField.textProperty().addListener((obs, o, n) -> {
            slugField.setText(n == null ? "" : generateSlug(n));
        });

        this.editing = PostListController.pendingEdit;
        PostListController.pendingEdit = null;

        if (editing == null) {
            formTitle.setText("New Post");
            saveButton.setText("Publish post");
        } else {
            formTitle.setText("Edit Post #" + editing.getId());
            saveButton.setText("Save changes");
            titleField.setText(editing.getTitle());
            authorIdField.setText(String.valueOf(editing.getAuthorId()));
            slugField.setText(editing.getSlug());
            imageField.setText(editing.getImage());
            contentArea.setText(editing.getContent());
        }
    }

    @FXML
    public void onSave() {
        String title = safe(titleField.getText());
        String content = safe(contentArea.getText());

        if (title.isEmpty())    { error("Title is required."); return; }
        if (title.length() < 3) { error("Title must be at least 3 characters."); return; }
        if (content.isEmpty())  { error("Content is required."); return; }

        int authorId;
        try {
            authorId = Integer.parseInt(authorIdField.getText().trim());
            if (authorId <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            error("Author ID must be a positive number."); return;
        }

        Post target = editing != null ? editing : new Post();
        target.setTitle(title);
        target.setAuthorId(authorId);
        target.setSlug(generateSlug(title));
        target.setContent(content);
        target.setImage(safe(imageField.getText()));
        if (editing == null) target.setCreatedAt(LocalDateTime.now());
        else target.setUpdatedAt(LocalDateTime.now());

        try {
            if (editing == null) service.ajouter(target);
            else service.modifier(target);
            onCancel();
        } catch (Exception e) { error("Save failed: " + e.getMessage()); }
    }

    @FXML public void onCancel() { Router.navigate("/fxml/PostList.fxml"); }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private String generateSlug(String text) {
        if (text == null) return "";
        return text.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Validation"); a.setHeaderText(null); a.setContentText(msg);
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        a.showAndWait();
    }
}

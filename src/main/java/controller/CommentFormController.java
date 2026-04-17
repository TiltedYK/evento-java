package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Comment;
import service.CommentService;

import java.time.LocalDateTime;

public class CommentFormController {

    @FXML private Label formTitle;
    @FXML private TextField postIdField;
    @FXML private TextField authorIdField;
    @FXML private TextArea contentArea;
    @FXML private Button saveButton;

    private final CommentService service = new CommentService();
    private Comment editing;

    public void setComment(Comment c) {
        this.editing = c;
        if (c == null) {
            formTitle.setText("New Comment");
            saveButton.setText("Post comment");
            return;
        }
        formTitle.setText("Edit Comment #" + c.getId());
        saveButton.setText("Save changes");

        postIdField.setText(String.valueOf(c.getPostId()));
        authorIdField.setText(String.valueOf(c.getAuthorId()));
        contentArea.setText(c.getContent());
    }

    @FXML
    public void onSave() {
        String content = safe(contentArea.getText());
        if (content.isEmpty())  { showError("Content is required."); return; }
        if (content.length() < 2) { showError("Comment is too short."); return; }
        if (content.length() > 5000) { showError("Comment is too long (max 5000 characters)."); return; }

        int postId, authorId;
        try {
            postId = Integer.parseInt(postIdField.getText().trim());
            authorId = Integer.parseInt(authorIdField.getText().trim());
            if (postId <= 0 || authorId <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            showError("Post ID and Author ID must be positive numbers.");
            return;
        }

        Comment target = editing != null ? editing : new Comment();
        target.setPostId(postId);
        target.setAuthorId(authorId);
        target.setContent(content);

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

    private void close() { ((Stage) postIdField.getScene().getWindow()).close(); }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Validation");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        a.showAndWait();
    }
}

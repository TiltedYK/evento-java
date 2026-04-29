package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Comment;
import service.CommentService;
import util.Router;

import java.time.LocalDateTime;

public class CommentFormController {

    @FXML private Label formTitle;
    @FXML private TextField postIdField;
    @FXML private TextField authorIdField;
    @FXML private TextArea contentArea;
    @FXML private Button saveButton;

    private final CommentService service = new CommentService();
    private Comment editing;

    @FXML
    public void initialize() {
        this.editing = CommentListController.pendingEdit;
        CommentListController.pendingEdit = null;

        if (editing == null) {
            formTitle.setText("New Comment");
            saveButton.setText("Post comment");
        } else {
            formTitle.setText("Edit Comment #" + editing.getId());
            saveButton.setText("Save changes");
            postIdField.setText(String.valueOf(editing.getPostId()));
            authorIdField.setText(String.valueOf(editing.getAuthorId()));
            contentArea.setText(editing.getContent());
        }
    }

    @FXML
    public void onSave() {
        String content = safe(contentArea.getText());
        if (content.isEmpty())      { error("Content is required."); return; }
        if (content.length() < 2)   { error("Comment is too short."); return; }
        if (content.length() > 5000){ error("Comment is too long (max 5000 characters)."); return; }

        int postId, authorId;
        try {
            postId = Integer.parseInt(postIdField.getText().trim());
            authorId = Integer.parseInt(authorIdField.getText().trim());
            if (postId <= 0 || authorId <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            error("Post ID and Author ID must be positive numbers."); return;
        }

        Comment target = editing != null ? editing : new Comment();
        target.setPostId(postId);
        target.setAuthorId(authorId);
        target.setContent(content);
        if (editing == null) target.setCreatedAt(LocalDateTime.now());
        else target.setUpdatedAt(LocalDateTime.now());

        try {
            if (editing == null) service.ajouter(target);
            else service.modifier(target);
            onCancel();
        } catch (Exception e) { error("Save failed: " + e.getMessage()); }
    }

    @FXML public void onCancel() { Router.navigate("/fxml/CommentList.fxml"); }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Validation"); a.setHeaderText(null); a.setContentText(msg);
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        a.showAndWait();
    }
}

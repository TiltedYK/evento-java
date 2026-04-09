package model;

import java.time.LocalDateTime;

public class Comment {

    private int id;
    private int postId;
    private int authorId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public Comment() {
        this.createdAt = LocalDateTime.now();
    }

    public Comment(int postId, int authorId, String content) {
        this.postId = postId;
        this.authorId = authorId;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", postId=" + postId +
                ", authorId=" + authorId +
                ", content='" + content + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

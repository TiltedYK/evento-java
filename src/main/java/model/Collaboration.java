package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Collaboration {

    private int id;
    private int partnerId;
    private String title;
    private String type;       // "image" or "video"
    private String fileName;
    private String linkUrl;
    private String position;   // "top" or "bottom"
    private String status;     // "pending", "approved", "rejected"
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private int viewCount;
    private Integer width;
    private Integer height;
    private Double price;

    public Collaboration() {
        this.type = "image";
        this.position = "top";
        this.status = "pending";
        this.viewCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    public Collaboration(int partnerId, String title, String type, String fileName, String linkUrl, String position, LocalDate startDate, LocalDate endDate) {
        this.partnerId = partnerId;
        this.title = title;
        this.type = type;
        this.fileName = fileName;
        this.linkUrl = linkUrl;
        this.position = position;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = "pending";
        this.viewCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPartnerId() { return partnerId; }
    public void setPartnerId(int partnerId) { this.partnerId = partnerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    @Override
    public String toString() {
        return "Collaboration{" +
                "id=" + id +
                ", partnerId=" + partnerId +
                ", title='" + title + '\'' +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", position='" + position + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", viewCount=" + viewCount +
                '}';
    }
}

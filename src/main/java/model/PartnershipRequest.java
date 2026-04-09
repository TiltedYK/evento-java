package model;

import java.time.LocalDateTime;

public class PartnershipRequest {

    private int id;
    private Integer userId;
    private String status;
    private String contactName;
    private String email;
    private String companyName;
    private String phone;
    private String message;
    private LocalDateTime createdAt;

    public PartnershipRequest() {
        this.status = "pending";
        this.createdAt = LocalDateTime.now();
    }

    public PartnershipRequest(String contactName, String email, String companyName, String phone, String message) {
        this.contactName = contactName;
        this.email = email;
        this.companyName = companyName;
        this.phone = phone;
        this.message = message;
        this.status = "pending";
        this.createdAt = LocalDateTime.now();
    }

    public PartnershipRequest(int id, Integer userId, String status, String contactName, String email, String companyName, String phone, String message, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.contactName = contactName;
        this.email = email;
        this.companyName = companyName;
        this.phone = phone;
        this.message = message;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "PartnershipRequest{" +
                "id=" + id +
                ", status='" + status + '\'' +
                ", contactName='" + contactName + '\'' +
                ", email='" + email + '\'' +
                ", companyName='" + companyName + '\'' +
                ", phone='" + phone + '\'' +
                ", message='" + message + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

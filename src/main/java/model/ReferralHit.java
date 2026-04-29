package model;

import java.time.LocalDateTime;

public class ReferralHit {

    private int id;
    private int influencerId;
    private String sessionId;
    private LocalDateTime visitedAt;
    private Integer referredUserId;
    
    // UI Only (Transient)
    private String collaborationTitle;
    private String referredUserEmail;

    public ReferralHit() {
        this.visitedAt = LocalDateTime.now();
    }

    public ReferralHit(int influencerId, String sessionId, Integer referredUserId) {
        this.influencerId = influencerId;
        this.sessionId = sessionId;
        this.referredUserId = referredUserId;
        this.visitedAt = LocalDateTime.now();
    }

    public ReferralHit(int id, int influencerId, String sessionId, LocalDateTime visitedAt, Integer referredUserId) {
        this.id = id;
        this.influencerId = influencerId;
        this.sessionId = sessionId;
        this.visitedAt = visitedAt;
        this.referredUserId = referredUserId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getInfluencerId() { return influencerId; }
    public void setInfluencerId(int influencerId) { this.influencerId = influencerId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public LocalDateTime getVisitedAt() { return visitedAt; }
    public void setVisitedAt(LocalDateTime visitedAt) { this.visitedAt = visitedAt; }

    public Integer getReferredUserId() { return referredUserId; }
    public void setReferredUserId(Integer referredUserId) { this.referredUserId = referredUserId; }

    public String getCollaborationTitle() { return collaborationTitle; }
    public void setCollaborationTitle(String collaborationTitle) { this.collaborationTitle = collaborationTitle; }

    public String getReferredUserEmail() { return referredUserEmail; }
    public void setReferredUserEmail(String referredUserEmail) { this.referredUserEmail = referredUserEmail; }

    @Override
    public String toString() {
        return "ReferralHit{" +
                "id=" + id +
                ", influencerId=" + influencerId +
                ", sessionId='" + sessionId + '\'' +
                ", visitedAt=" + visitedAt +
                ", referredUserId=" + referredUserId +
                '}';
    }
}

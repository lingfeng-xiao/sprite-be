package com.lingfeng.sprite.life.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "life_runtime_state")
public class LifeRuntimeStateEntity {

    @Id
    private Long id;

    @Lob
    @Column(name = "identity_json", nullable = false, columnDefinition = "LONGTEXT")
    private String identityJson;

    @Lob
    @Column(name = "self_json", nullable = false, columnDefinition = "LONGTEXT")
    private String selfJson;

    @Lob
    @Column(name = "relationship_json", nullable = false, columnDefinition = "LONGTEXT")
    private String relationshipJson;

    @Lob
    @Column(name = "goals_json", nullable = false, columnDefinition = "LONGTEXT")
    private String goalsJson;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIdentityJson() { return identityJson; }
    public void setIdentityJson(String identityJson) { this.identityJson = identityJson; }
    public String getSelfJson() { return selfJson; }
    public void setSelfJson(String selfJson) { this.selfJson = selfJson; }
    public String getRelationshipJson() { return relationshipJson; }
    public void setRelationshipJson(String relationshipJson) { this.relationshipJson = relationshipJson; }
    public String getGoalsJson() { return goalsJson; }
    public void setGoalsJson(String goalsJson) { this.goalsJson = goalsJson; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

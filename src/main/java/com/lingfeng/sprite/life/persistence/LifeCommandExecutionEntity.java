package com.lingfeng.sprite.life.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "life_command_executions")
public class LifeCommandExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "command_id", nullable = false, unique = true)
    private String commandId;

    @Column(name = "command_type", nullable = false)
    private String commandType;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Lob
    @Column(name = "context_json", columnDefinition = "LONGTEXT")
    private String contextJson;

    @Column(nullable = false)
    private String source;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String summary;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String detail;

    @Column(nullable = false)
    private boolean success;

    @Lob
    @Column(name = "impact_json", columnDefinition = "LONGTEXT")
    private String impactJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCommandId() { return commandId; }
    public void setCommandId(String commandId) { this.commandId = commandId; }
    public String getCommandType() { return commandType; }
    public void setCommandType(String commandType) { this.commandType = commandType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getContextJson() { return contextJson; }
    public void setContextJson(String contextJson) { this.contextJson = contextJson; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getImpactJson() { return impactJson; }
    public void setImpactJson(String impactJson) { this.impactJson = impactJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

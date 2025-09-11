package com.amfk.starfish.sync.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_details", indexes = {
    @Index(name = "idx_job_name_create_ts", columnList = "job_name, create_ts"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_create_ts", columnList = "create_ts")
})
public class JobExecution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "job_id", nullable = false, length = 36, unique = true)
    private String jobId;
    
    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;
    
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private JobStatus status;
    
    @Column(name = "start_time", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime startTime;
    
    @Column(name = "end_time", columnDefinition = "DATETIME(6)")
    private LocalDateTime endTime;
    
    @Column(name = "duration_ms", columnDefinition = "BIGINT")
    private Long durationMs;
    
    @Column(name = "records_processed", columnDefinition = "INT DEFAULT 0")
    private Integer recordsProcessed;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "api_response", columnDefinition = "JSON")
    private String apiResponse;
    
    @Column(name = "create_ts", nullable = false, columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime createTs;
    
    @Column(name = "update_ts", nullable = false, columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)")
    private LocalDateTime updateTs;
    
    public enum JobStatus {
        RUNNING, COMPLETED, FAILED, CANCELLED
    }
    
    // Default constructor
    public JobExecution() {
        this.createTs = LocalDateTime.now();
        this.updateTs = LocalDateTime.now();
    }
    
    // Constructor with required fields
    public JobExecution(String jobId, String jobName) {
        this();
        this.jobId = jobId;
        this.jobName = jobName;
        this.status = JobStatus.RUNNING;
        this.startTime = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getJobId() {
        return jobId;
    }
    
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
    
    public String getJobName() {
        return jobName;
    }
    
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }
    
    public JobStatus getStatus() {
        return status;
    }
    
    public void setStatus(JobStatus status) {
        this.status = status;
        this.updateTs = LocalDateTime.now();
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        this.updateTs = LocalDateTime.now();
    }
    
    public Long getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }
    
    public Integer getRecordsProcessed() {
        return recordsProcessed;
    }
    
    public void setRecordsProcessed(Integer recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.updateTs = LocalDateTime.now();
    }
    
    public String getApiResponse() {
        return apiResponse;
    }
    
    public void setApiResponse(String apiResponse) {
        this.apiResponse = apiResponse;
        this.updateTs = LocalDateTime.now();
    }
    
    public LocalDateTime getCreateTs() {
        return createTs;
    }
    
    public void setCreateTs(LocalDateTime createTs) {
        this.createTs = createTs;
    }
    
    public LocalDateTime getUpdateTs() {
        return updateTs;
    }
    
    public void setUpdateTs(LocalDateTime updateTs) {
        this.updateTs = updateTs;
    }
    
    public void complete() {
        this.status = JobStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
        this.durationMs = java.time.Duration.between(this.startTime, this.endTime).toMillis();
        this.updateTs = LocalDateTime.now();
    }
    
    public void fail(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.endTime = LocalDateTime.now();
        this.durationMs = java.time.Duration.between(this.startTime, this.endTime).toMillis();
        this.errorMessage = errorMessage;
        this.updateTs = LocalDateTime.now();
    }
} 
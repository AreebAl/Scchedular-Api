package com.amfk.starfish.sync.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class SiteDto {
    
    @JsonProperty("siteId")
    private String siteId;
    
    @JsonProperty("siteName")
    private String siteName;
    
    @JsonProperty("clusterId")
    private String clusterId;
    
    @JsonProperty("clusterName")
    private String clusterName;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("location")
    private String location;
    
    @JsonProperty("createdDate")
    private LocalDateTime createdDate;
    
    @JsonProperty("lastModifiedDate")
    private LocalDateTime lastModifiedDate;
    
    // Default constructor
    public SiteDto() {}
    
    // Constructor with required fields
    public SiteDto(String siteId, String siteName, String clusterId, String clusterName) {
        this.siteId = siteId;
        this.siteName = siteName;
        this.clusterId = clusterId;
        this.clusterName = clusterName;
    }
    
    // Getters and Setters
    public String getSiteId() {
        return siteId;
    }
    
    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }
    
    public String getSiteName() {
        return siteName;
    }
    
    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }
    
    public String getClusterId() {
        return clusterId;
    }
    
    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }
    
    public String getClusterName() {
        return clusterName;
    }
    
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
    
    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }
    
    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
    
    @Override
    public String toString() {
        return "SiteDto{" +
                "siteId='" + siteId + '\'' +
                ", siteName='" + siteName + '\'' +
                ", clusterId='" + clusterId + '\'' +
                ", clusterName='" + clusterName + '\'' +
                ", status='" + status + '\'' +
                ", location='" + location + '\'' +
                ", createdDate=" + createdDate +
                ", lastModifiedDate=" + lastModifiedDate +
                '}';
    }
} 
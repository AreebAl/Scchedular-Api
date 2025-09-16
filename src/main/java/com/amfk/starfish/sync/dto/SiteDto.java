package com.amfk.starfish.sync.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class SiteDto {
    
    @JsonProperty("id")
    private Integer id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("nameEnglish")
    private String nameEnglish;
    
    @JsonProperty("nameGerman")
    private String nameGerman;
    
    @JsonProperty("locationCode")
    private String locationCode;
    
    @JsonProperty("city")
    private String city;
    
    @JsonProperty("street")
    private String street;
    
    @JsonProperty("remark")
    private String remark;
    
    @JsonProperty("active")
    private Integer active;
    
    @JsonProperty("logCreatedBy")
    private String logCreatedBy;
    
    @JsonProperty("logCreatedOn")
    private LocalDateTime logCreatedOn;
    
    @JsonProperty("logUpdatedBy")
    private String logUpdatedBy;
    
    @JsonProperty("logUpdatedOn")
    private LocalDateTime logUpdatedOn;
    
    @JsonProperty("clusterName")
    private String clusterName;
    
    @JsonProperty("clusterId")
    private Integer clusterId;
    
    @JsonProperty("sipDomain")
    private String sipDomain;
    
    @JsonProperty("routingPolicy")
    private String routingPolicy;
    
    @JsonProperty("cmName")
    private String cmName;
    
    @JsonProperty("notes")
    private String notes;
    
    @JsonProperty("ars")
    private String ars;
    
    @JsonProperty("userStamp")
    private String userStamp;
    
    @JsonProperty("timeStamp")
    private LocalDateTime timeStamp;
    
    // Computed fields
    private String location;
    
    // Default constructor
    public SiteDto() {}
    
    // Constructor with required fields
    public SiteDto(Integer id, String name, String clusterName, Integer clusterId) {
        this.id = id;
        this.name = name;
        this.clusterName = clusterName;
        this.clusterId = clusterId;
    }
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getNameEnglish() {
        return nameEnglish;
    }
    
    public void setNameEnglish(String nameEnglish) {
        this.nameEnglish = nameEnglish;
    }
    
    public String getNameGerman() {
        return nameGerman;
    }
    
    public void setNameGerman(String nameGerman) {
        this.nameGerman = nameGerman;
    }
    
    public String getLocationCode() {
        return locationCode;
    }
    
    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getStreet() {
        return street;
    }
    
    public void setStreet(String street) {
        this.street = street;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
    
    public Integer getActive() {
        return active;
    }
    
    public void setActive(Integer active) {
        this.active = active;
    }
    
    public String getLogCreatedBy() {
        return logCreatedBy;
    }
    
    public void setLogCreatedBy(String logCreatedBy) {
        this.logCreatedBy = logCreatedBy;
    }
    
    public LocalDateTime getLogCreatedOn() {
        return logCreatedOn;
    }
    
    public void setLogCreatedOn(LocalDateTime logCreatedOn) {
        this.logCreatedOn = logCreatedOn;
    }
    
    public String getLogUpdatedBy() {
        return logUpdatedBy;
    }
    
    public void setLogUpdatedBy(String logUpdatedBy) {
        this.logUpdatedBy = logUpdatedBy;
    }
    
    public LocalDateTime getLogUpdatedOn() {
        return logUpdatedOn;
    }
    
    public void setLogUpdatedOn(LocalDateTime logUpdatedOn) {
        this.logUpdatedOn = logUpdatedOn;
    }
    
    public String getClusterName() {
        return clusterName;
    }
    
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }
    
    public Integer getClusterId() {
        return clusterId;
    }
    
    public void setClusterId(Integer clusterId) {
        this.clusterId = clusterId;
    }
    
    public String getSipDomain() {
        return sipDomain;
    }
    
    public void setSipDomain(String sipDomain) {
        this.sipDomain = sipDomain;
    }
    
    public String getRoutingPolicy() {
        return routingPolicy;
    }
    
    public void setRoutingPolicy(String routingPolicy) {
        this.routingPolicy = routingPolicy;
    }
    
    public String getCmName() {
        return cmName;
    }
    
    public void setCmName(String cmName) {
        this.cmName = cmName;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getArs() {
        return ars;
    }
    
    public void setArs(String ars) {
        this.ars = ars;
    }
    
    public String getUserStamp() {
        return userStamp;
    }
    
    public void setUserStamp(String userStamp) {
        this.userStamp = userStamp;
    }
    
    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }
    
    public void setTimeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    // Convenience methods for backward compatibility
    public String getSiteId() {
        return id != null ? id.toString() : null;
    }
    
    public String getSiteName() {
        return name;
    }
    
    public String getSiteCode() {
        return locationCode;
    }
    
    public String getStatus() {
        return active != null ? (active == 1 ? "ACTIVE" : "INACTIVE") : null;
    }
    
    @Override
    public String toString() {
        return "SiteDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", locationCode='" + locationCode + '\'' +
                ", clusterId=" + clusterId +
                ", clusterName='" + clusterName + '\'' +
                ", active=" + active +
                ", city='" + city + '\'' +
                ", street='" + street + '\'' +
                ", location='" + location + '\'' +
                ", logCreatedOn=" + logCreatedOn +
                ", logUpdatedOn=" + logUpdatedOn +
                '}';
    }
}

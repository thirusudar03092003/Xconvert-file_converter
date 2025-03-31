package com.xconvert.model;

import java.util.Date;
import java.util.UUID;

public class BatchJobItem {
    private String id;
    private String originalFilename;
    private String storedFilename;
    private String sourceFormat;
    private String outputFilename;
    private String status; // "pending", "processing", "completed", "failed"
    private String errorMessage;
    private Date startTime;
    private Date endTime;
    
    public BatchJobItem() {
        this.id = UUID.randomUUID().toString();
        this.status = "pending";
    }
    
    public BatchJobItem(String originalFilename, String storedFilename, String sourceFormat) {
        this();
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.sourceFormat = sourceFormat;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public String getSourceFormat() {
        return sourceFormat;
    }

    public void setSourceFormat(String sourceFormat) {
        this.sourceFormat = sourceFormat;
    }

    public String getOutputFilename() {
        return outputFilename;
    }

    public void setOutputFilename(String outputFilename) {
        this.outputFilename = outputFilename;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
    
    public long getProcessingTimeMs() {
        if (startTime != null && endTime != null) {
            return endTime.getTime() - startTime.getTime();
        }
        return 0;
    }
}

package com.xconvert.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class BatchJob {
    private String id;
    private String targetFormat;
    private Date creationTime;
    private String status; // "pending", "processing", "completed", "failed"
    private List<BatchJobItem> items;
    
    public BatchJob() {
        this.id = UUID.randomUUID().toString();
        this.creationTime = new Date();
        this.status = "pending";
        this.items = new ArrayList<>();
    }
    
    public BatchJob(String targetFormat) {
        this();
        this.targetFormat = targetFormat;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(String targetFormat) {
        this.targetFormat = targetFormat;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<BatchJobItem> getItems() {
        return items;
    }

    public void setItems(List<BatchJobItem> items) {
        this.items = items;
    }
    
    public void addItem(BatchJobItem item) {
        this.items.add(item);
    }
    
    public int getTotalItems() {
        return items.size();
    }
    
    public int getCompletedItems() {
        return (int) items.stream()
                .filter(item -> "completed".equals(item.getStatus()))
                .count();
    }
    
    public int getFailedItems() {
        return (int) items.stream()
                .filter(item -> "failed".equals(item.getStatus()))
                .count();
    }
    
    public boolean isCompleted() {
        return getCompletedItems() + getFailedItems() == getTotalItems();
    }
    
    public void updateStatus() {
        if (isCompleted()) {
            if (getFailedItems() == getTotalItems()) {
                this.status = "failed";
            } else if (getFailedItems() > 0) {
                this.status = "completed_with_errors";
            } else {
                this.status = "completed";
            }
        } else if (getCompletedItems() > 0 || getFailedItems() > 0) {
            this.status = "processing";
        }
    }
}

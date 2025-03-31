package com.xconvert.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskService {
    
    private final Map<String, String> taskStatus = new ConcurrentHashMap<>();
    
    public void setStatus(String taskId, String status) {
        taskStatus.put(taskId, status);
    }
    
    public String getStatus(String taskId) {
        return taskStatus.getOrDefault(taskId, "unknown");
    }
    
    public void setError(String taskId, String errorMessage) {
        taskStatus.put(taskId, "error:" + errorMessage);
    }
    
    public void setCompleted(String taskId, String result) {
        taskStatus.put(taskId, "completed:" + result);
    }
}

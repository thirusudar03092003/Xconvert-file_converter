package com.xconvert.controller;

import com.xconvert.model.ConversionRequest;
import com.xconvert.service.ConversionService;
import com.xconvert.service.TaskService;
import com.xconvert.util.JsonFixer;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/convert")
@CrossOrigin(origins = "*")
public class ConversionController {

    private static final Logger logger = LoggerFactory.getLogger(ConversionController.class);
    private final ConversionService conversionService;
    private final TaskService taskService;

    @Autowired
    public ConversionController(ConversionService conversionService, TaskService taskService) {
        this.conversionService = conversionService;
        this.taskService = taskService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file,
                                            @RequestParam("sourceFormat") String sourceFormat,
                                            @RequestParam("targetFormat") String targetFormat) {
        logger.info("Received upload request: file={}, sourceFormat={}, targetFormat={}",
                file.getOriginalFilename(), sourceFormat, targetFormat);
        
        try {
            // Auto-detect format if not provided
            if (sourceFormat == null || sourceFormat.isEmpty()) {
                String extension = FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase();
                sourceFormat = extension;
                logger.info("Auto-detected source format: {}", sourceFormat);
            }
            
            String fileName = conversionService.saveFile(file);
            logger.info("File saved successfully: {}", fileName);
            
            // Generate a task ID
            String taskId = UUID.randomUUID().toString();
            taskService.setStatus(taskId, "uploaded");
            
            // Start async conversion
            processFileAsync(taskId, fileName, sourceFormat, targetFormat);
            
            return ResponseEntity.ok().body("{\"taskId\": \"" + taskId + "\", \"fileName\": \"" + fileName + "\"}");
        } catch (IOException e) {
            logger.error("Error uploading file", e);
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    @Async("taskExecutor")
    public void processFileAsync(String taskId, String fileName, String sourceFormat, String targetFormat) {
        logger.info("Starting async processing for task: {}", taskId);
        taskService.setStatus(taskId, "processing");
        
        try {
            String outputFileName = conversionService.convertFile(fileName, sourceFormat, targetFormat);
            taskService.setCompleted(taskId, outputFileName);
            logger.info("Async processing completed for task: {}", taskId);
        } catch (Exception e) {
            logger.error("Error in async processing for task: {}", taskId, e);
            taskService.setError(taskId, e.getMessage());
        }
    }

    @GetMapping("/status/{taskId}")
    public ResponseEntity<String> getStatus(@PathVariable String taskId) {
        String status = taskService.getStatus(taskId);
        logger.info("Status for task {}: {}", taskId, status);
        
        if (status.startsWith("completed:")) {
            String outputFileName = status.substring("completed:".length());
            return ResponseEntity.ok().body("{\"status\": \"completed\", \"outputFileName\": \"" + outputFileName + "\"}");
        } else if (status.startsWith("error:")) {
            String error = status.substring("error:".length());
            return ResponseEntity.ok().body("{\"status\": \"error\", \"error\": \"" + error + "\"}");
        } else {
            return ResponseEntity.ok().body("{\"status\": \"" + status + "\"}");
        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        logger.info("Received download request for file: {}", fileName);
        
        try {
            Resource resource = conversionService.loadFileAsResource(fileName);
            
            // Determine content type
            String contentType;
            if (fileName.endsWith(".csv")) {
                contentType = "text/csv";
            } else if (fileName.endsWith(".json")) {
                contentType = "application/json";
            } else if (fileName.endsWith(".xml")) {
                contentType = "application/xml";
            } else {
                contentType = "application/octet-stream";
            }
            
            logger.info("File found, preparing download with content type: {}", contentType);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error downloading file", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    // Test endpoint for direct JSON to CSV conversion
    @PostMapping("/test/json-to-csv")
    public ResponseEntity<String> testJsonToCsv(@RequestBody String jsonContent) {
        logger.info("Received test JSON to CSV request");
        
        try {
            // Fix the JSON
            jsonContent = JsonFixer.fixJson(jsonContent);
            
            // Parse the JSON
            JSONArray jsonArray = new JSONArray(jsonContent);
            
            // Get all keys
            Set<String> allKeys = new HashSet<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    allKeys.add(keys.next());
                }
            }
            
            // Create CSV
            StringBuilder csvBuilder = new StringBuilder();
            
            // Headers
            String[] headers = allKeys.toArray(new String[0]);
            for (int i = 0; i < headers.length; i++) {
                csvBuilder.append(headers[i]);
                if (i < headers.length - 1) {
                    csvBuilder.append(",");
                }
            }
            csvBuilder.append("\n");
            
            // Data rows
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                
                for (int j = 0; j < headers.length; j++) {
                    String key = headers[j];
                    if (jsonObject.has(key)) {
                        String value = jsonObject.get(key).toString();
                        if (value.contains(",")) {
                            value = "\"" + value + "\"";
                        }
                        csvBuilder.append(value);
                    }
                    
                    if (j < headers.length - 1) {
                        csvBuilder.append(",");
                    }
                }
                
                csvBuilder.append("\n");
            }
            
            return ResponseEntity.ok().body(csvBuilder.toString());
        } catch (Exception e) {
            logger.error("Error in test conversion", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}

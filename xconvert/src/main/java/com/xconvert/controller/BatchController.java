package com.xconvert.controller;

import com.xconvert.model.BatchJob;
import com.xconvert.model.BatchJobItem;
import com.xconvert.service.BatchJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/batch")
@CrossOrigin(origins = "*")
public class BatchController {
    private static final Logger logger = LoggerFactory.getLogger(BatchController.class);
    
    @Autowired
    private BatchJobService batchJobService;
    
    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createBatchJob(@RequestParam("targetFormat") String targetFormat) {
        logger.info("Creating batch job with target format: {}", targetFormat);
        
        BatchJob batchJob = batchJobService.createBatchJob(targetFormat);
        
        Map<String, String> response = new HashMap<>();
        response.put("batchJobId", batchJob.getId());
        response.put("targetFormat", batchJob.getTargetFormat());
        response.put("status", batchJob.getStatus());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/upload/{batchJobId}")
    public ResponseEntity<Map<String, String>> uploadFile(
            @PathVariable String batchJobId,
            @RequestParam("file") MultipartFile file) {
        
        logger.info("Uploading file to batch job {}: {}", batchJobId, file.getOriginalFilename());
        
        try {
            BatchJobItem item = batchJobService.addFileToBatchJob(batchJobId, file);
            
            Map<String, String> response = new HashMap<>();
            response.put("batchJobId", batchJobId);
            response.put("itemId", item.getId());
            response.put("filename", item.getOriginalFilename());
            response.put("status", item.getStatus());
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            logger.error("Error uploading file to batch job {}: {}", batchJobId, e.getMessage());
            
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to upload file: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/process/{batchJobId}")
    public ResponseEntity<Map<String, String>> processBatchJob(@PathVariable String batchJobId) {
        logger.info("Processing batch job: {}", batchJobId);
        
        BatchJob batchJob = batchJobService.getBatchJob(batchJobId);
        if (batchJob == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Batch job not found: " + batchJobId);
            return ResponseEntity.badRequest().body(response);
        }
        
        batchJobService.processBatchJob(batchJobId);
        
        Map<String, String> response = new HashMap<>();
        response.put("batchJobId", batchJobId);
        response.put("status", "processing");
        response.put("totalItems", String.valueOf(batchJob.getTotalItems()));
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status/{batchJobId}")
    public ResponseEntity<Map<String, Object>> getBatchJobStatus(@PathVariable String batchJobId) {
        logger.info("Getting status for batch job: {}", batchJobId);
        
        BatchJob batchJob = batchJobService.getBatchJob(batchJobId);
        if (batchJob == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Batch job not found: " + batchJobId);
            return ResponseEntity.badRequest().body(response);
        }
        
        batchJob.updateStatus();
        
        Map<String, Object> response = new HashMap<>();
        response.put("batchJobId", batchJob.getId());
        response.put("status", batchJob.getStatus());
        response.put("targetFormat", batchJob.getTargetFormat());
        response.put("creationTime", batchJob.getCreationTime());
        response.put("totalItems", batchJob.getTotalItems());
        response.put("completedItems", batchJob.getCompletedItems());
        response.put("failedItems", batchJob.getFailedItems());
        response.put("items", batchJob.getItems());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/download/{batchJobId}")
    public ResponseEntity<Resource> downloadBatchResults(@PathVariable String batchJobId) {
        logger.info("Downloading results for batch job: {}", batchJobId);
        
        BatchJob batchJob = batchJobService.getBatchJob(batchJobId);
        if (batchJob == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (!"completed".equals(batchJob.getStatus()) && !"completed_with_errors".equals(batchJob.getStatus())) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            // Create a ZIP file containing all converted files
            Path tempDir = Paths.get("/mnt/d/Xconvert/temp");
            Files.createDirectories(tempDir);
            
            String zipFilename = "batch_" + batchJobId + ".zip";
            Path zipPath = tempDir.resolve(zipFilename);
            
            try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                Path convertedDir = Paths.get("/mnt/d/Xconvert/converted");
                
                for (BatchJobItem item : batchJob.getItems()) {
                    if ("completed".equals(item.getStatus()) && item.getOutputFilename() != null) {
                        Path filePath = convertedDir.resolve(item.getOutputFilename());
                        
                        if (Files.exists(filePath)) {
                            ZipEntry zipEntry = new ZipEntry(item.getOriginalFilename().replace("." + item.getSourceFormat(), "." + batchJob.getTargetFormat()));
                            zipOut.putNextEntry(zipEntry);
                            Files.copy(filePath, zipOut);
                            zipOut.closeEntry();
                        }
                    }
                }
            }
            
            Resource resource = new UrlResource(zipPath.toUri());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFilename + "\"")
                    .body(resource);
        } catch (IOException e) {
            logger.error("Error creating ZIP file for batch job {}: {}", batchJobId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}

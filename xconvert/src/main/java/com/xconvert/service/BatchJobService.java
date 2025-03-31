package com.xconvert.service;

import com.xconvert.model.BatchJob;
import com.xconvert.model.BatchJobItem;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BatchJobService {
    private static final Logger logger = LoggerFactory.getLogger(BatchJobService.class);
    
    private final Map<String, BatchJob> batchJobs = new ConcurrentHashMap<>();
    private final Path uploadDirectory;
    private final Path convertedDirectory;
    
    @Autowired
    private ConversionService conversionService;
    
    public BatchJobService() throws IOException {
        this.uploadDirectory = Paths.get("/mnt/d/Xconvert/uploads").toAbsolutePath().normalize();
        this.convertedDirectory = Paths.get("/mnt/d/Xconvert/converted").toAbsolutePath().normalize();
        
        Files.createDirectories(uploadDirectory);
        Files.createDirectories(convertedDirectory);
    }
    
    public BatchJob createBatchJob(String targetFormat) {
        BatchJob batchJob = new BatchJob(targetFormat);
        batchJobs.put(batchJob.getId(), batchJob);
        return batchJob;
    }
    
    public BatchJob getBatchJob(String batchJobId) {
        return batchJobs.get(batchJobId);
    }
    
    public BatchJobItem addFileToBatchJob(String batchJobId, MultipartFile file) throws IOException {
        BatchJob batchJob = batchJobs.get(batchJobId);
        if (batchJob == null) {
            throw new IllegalArgumentException("Batch job not found: " + batchJobId);
        }
        
        String originalFilename = file.getOriginalFilename();
        String sourceFormat = FilenameUtils.getExtension(originalFilename);
        String storedFilename = UUID.randomUUID().toString() + "." + sourceFormat;
        
        Path targetLocation = uploadDirectory.resolve(storedFilename);
        Files.copy(file.getInputStream(), targetLocation);
        
        BatchJobItem item = new BatchJobItem(originalFilename, storedFilename, sourceFormat);
        batchJob.addItem(item);
        
        return item;
    }
    
    @Async
    public void processBatchJob(String batchJobId) {
        BatchJob batchJob = batchJobs.get(batchJobId);
        if (batchJob == null) {
            logger.error("Batch job not found: {}", batchJobId);
            return;
        }
        
        batchJob.setStatus("processing");
        
        for (BatchJobItem item : batchJob.getItems()) {
            processItem(batchJob, item);
            batchJob.updateStatus();
        }
    }
    
    private void processItem(BatchJob batchJob, BatchJobItem item) {
        item.setStatus("processing");
        item.setStartTime(new Date());
        
        try {
            Path sourcePath = uploadDirectory.resolve(item.getStoredFilename());
            String outputFilename = FilenameUtils.getBaseName(item.getStoredFilename()) + "." + batchJob.getTargetFormat();
            Path targetPath = convertedDirectory.resolve(outputFilename);
            
            conversionService.convertFile(
                    item.getStoredFilename(),
                    item.getSourceFormat(),
                    batchJob.getTargetFormat()
            );
            
            item.setOutputFilename(outputFilename);
            item.setStatus("completed");
        } catch (Exception e) {
            logger.error("Error processing batch job item: {}", item.getId(), e);
            item.setStatus("failed");
            item.setErrorMessage(e.getMessage());
        } finally {
            item.setEndTime(new Date());
        }
    }
    
    public void clearCompletedJobs() {
        batchJobs.entrySet().removeIf(entry -> {
            BatchJob job = entry.getValue();
            return "completed".equals(job.getStatus()) || "failed".equals(job.getStatus());
        });
    }
}

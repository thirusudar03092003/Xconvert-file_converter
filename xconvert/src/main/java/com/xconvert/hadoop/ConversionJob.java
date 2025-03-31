package com.xconvert.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
public class ConversionJob {
    private static final Logger logger = LoggerFactory.getLogger(ConversionJob.class);
    
    // Thread pool for parallel job execution
    private final ExecutorService executorService;
    
    public ConversionJob() {
        // Create a thread pool with a fixed number of threads
        // This allows multiple Hadoop jobs to run in parallel
        this.executorService = Executors.newFixedThreadPool(5);
    }
    
    public boolean runJob(String inputPath, String outputPath, String sourceFormat, String targetFormat) 
            throws IOException, InterruptedException, ClassNotFoundException {
        
        Configuration conf = new Configuration();
        conf.set("sourceFormat", sourceFormat);
        conf.set("targetFormat", targetFormat);
        
        Job job = Job.getInstance(conf, "File Format Conversion: " + sourceFormat + " to " + targetFormat);
        job.setJarByClass(ConversionJob.class);
        
        // Set mapper and reducer based on conversion type
        if ("csv".equals(sourceFormat) && "json".equals(targetFormat)) {
            job.setMapperClass(CSVToJSONMapper.class);
            job.setReducerClass(CSVToJSONReducer.class);
        }
        // Add more conversion types as we implement them
        // For now, we'll only use the existing CSV to JSON conversion
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        
        FileInputFormat.addInputPath(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath + "_temp"));
        
        boolean success = job.waitForCompletion(true);
        
        // Post-processing: Combine the output files into a single file
        if (success) {
            // Implement file merging logic here
        }
        
        return success;
    }
    
    public Future<Boolean> runJobAsync(String inputPath, String outputPath, String sourceFormat, String targetFormat) {
        return executorService.submit(() -> {
            try {
                return runJob(inputPath, outputPath, sourceFormat, targetFormat);
            } catch (Exception e) {
                logger.error("Error running Hadoop job", e);
                return false;
            }
        });
    }
    
    // Method to shut down the executor service
    public void shutdown() {
        executorService.shutdown();
    }
}

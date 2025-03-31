package com.xconvert.service;

import com.xconvert.hadoop.ConversionJob;
import com.xconvert.util.JsonFixer;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Service
public class ConversionService {

    private static final Logger logger = LoggerFactory.getLogger(ConversionService.class);
    
    private final Path fileStorageLocation;
    private final Path outputStorageLocation;
    private final ConversionJob conversionJob;

    public ConversionService() throws IOException {
        // Create directories with absolute paths
        this.fileStorageLocation = Paths.get("/mnt/d/Xconvert/uploads").toAbsolutePath().normalize();
        this.outputStorageLocation = Paths.get("/mnt/d/Xconvert/converted").toAbsolutePath().normalize();
        
        logger.info("Upload directory: {}", fileStorageLocation);
        logger.info("Output directory: {}", outputStorageLocation);
        
        // Create directories if they don't exist
        File uploadDir = fileStorageLocation.toFile();
        File convertedDir = outputStorageLocation.toFile();
        
        if (!uploadDir.exists()) {
            logger.info("Creating upload directory");
            uploadDir.mkdirs();
        }
        
        if (!convertedDir.exists()) {
            logger.info("Creating converted directory");
            convertedDir.mkdirs();
        }
        
        this.conversionJob = new ConversionJob();
    }

    public String saveFile(MultipartFile file) throws IOException {
        logger.info("Saving file: {}", file.getOriginalFilename());
        
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file");
        }
        
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String uniqueFilename = UUID.randomUUID().toString() + "." + FilenameUtils.getExtension(originalFilename);
        
        Path targetLocation = fileStorageLocation.resolve(uniqueFilename);
        logger.info("Saving to location: {}", targetLocation);
        
        // Use transferTo instead of copy for better handling of large files
        file.transferTo(targetLocation);
        logger.info("File saved successfully");
        
        return uniqueFilename;
    }

    public String convertFile(String fileName, String sourceFormat, String targetFormat) throws Exception {
        logger.info("Converting file: {} from {} to {}", fileName, sourceFormat, targetFormat);
        
        Path sourcePath = fileStorageLocation.resolve(fileName);
        String outputFileName = FilenameUtils.getBaseName(fileName) + "." + targetFormat.toLowerCase();
        Path targetPath = outputStorageLocation.resolve(outputFileName);
        
        logger.info("Source path: {}", sourcePath);
        logger.info("Target path: {}", targetPath);
        
        // Check file size for large file handling
        long fileSize = Files.size(sourcePath);
        logger.info("File size: {} bytes", fileSize);
        
        // CSV to JSON conversion
        if ("csv".equals(sourceFormat) && "json".equals(targetFormat)) {
            logger.info("Performing CSV to JSON conversion");
            
            // For larger files, use Hadoop
            if (fileSize > 10 * 1024 * 1024) { // > 10MB
                logger.info("Large file detected, using Hadoop for processing");
                try {
                    conversionJob.runJob(
                            sourcePath.toString(),
                            targetPath.toString(),
                            sourceFormat.toLowerCase(),
                            targetFormat.toLowerCase()
                    );
                    logger.info("Hadoop conversion completed successfully");
                    return outputFileName;
                } catch (Exception e) {
                    logger.error("Error during Hadoop conversion", e);
                    // Fall back to direct conversion
                }
            }
            
            // Direct conversion for smaller files or as fallback
            try (BufferedReader reader = new BufferedReader(new FileReader(sourcePath.toFile()))) {
                StringBuilder jsonBuilder = new StringBuilder("[\n");
                
                // Read header
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    throw new IOException("Empty CSV file");
                }
                
                String[] headers = headerLine.split(",");
                boolean firstRow = true;
                
                // Process data rows
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!firstRow) {
                        jsonBuilder.append(",\n");
                    }
                    
                    String[] values = line.split(",");
                    jsonBuilder.append("  {\n");
                    
                    for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                        jsonBuilder.append("    \"").append(headers[i].trim()).append("\": \"")
                                .append(values[i].trim()).append("\"");
                        
                        if (i < Math.min(headers.length, values.length) - 1) {
                            jsonBuilder.append(",");
                        }
                        jsonBuilder.append("\n");
                    }
                    
                    jsonBuilder.append("  }");
                    firstRow = false;
                }
                
                jsonBuilder.append("\n]");
                Files.write(targetPath, jsonBuilder.toString().getBytes());
                logger.info("CSV to JSON conversion completed successfully");
            }
            
            return outputFileName;
        }
        
        // CSV to XML conversion
        else if ("csv".equals(sourceFormat) && "xml".equals(targetFormat)) {
            logger.info("Performing CSV to XML conversion");
            
            try (BufferedReader reader = new BufferedReader(new FileReader(sourcePath.toFile()))) {
                // Create XML document
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                Document doc = docBuilder.newDocument();
                
                // Create root element
                Element rootElement = doc.createElement("records");
                doc.appendChild(rootElement);
                
                // Read header
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    throw new IOException("Empty CSV file");
                }
                
                String[] headers = headerLine.split(",");
                
                // Process data rows
                String line;
                int recordId = 1;
                while ((line = reader.readLine()) != null) {
                    String[] values = line.split(",");
                    
                    Element recordElement = doc.createElement("record");
                    recordElement.setAttribute("id", String.valueOf(recordId++));
                    rootElement.appendChild(recordElement);
                    
                    for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                        String header = headers[i].trim();
                        String value = values[i].trim();
                        
                        // Create valid XML element name (replace spaces and special chars)
                        String elementName = header.replaceAll("[^a-zA-Z0-9]", "_");
                        if (elementName.isEmpty() || !Character.isLetter(elementName.charAt(0))) {
                            elementName = "field_" + elementName;
                        }
                        
                        Element fieldElement = doc.createElement(elementName);
                        fieldElement.setTextContent(value);
                        recordElement.appendChild(fieldElement);
                    }
                }
                
                // Write XML to file
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(targetPath.toFile());
                transformer.transform(source, result);
                
                logger.info("CSV to XML conversion completed successfully");
            } catch (ParserConfigurationException | TransformerException e) {
                logger.error("Error creating XML", e);
                throw new RuntimeException("Error creating XML: " + e.getMessage());
            }
            
            return outputFileName;
        }
        
        // CSV to TXT conversion
        else if ("csv".equals(sourceFormat) && "txt".equals(targetFormat)) {
            logger.info("Performing CSV to TXT conversion");
            
            try (BufferedReader reader = new BufferedReader(new FileReader(sourcePath.toFile()))) {
                StringBuilder txtBuilder = new StringBuilder();
                
                // Read header
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    throw new IOException("Empty CSV file");
                }
                
                String[] headers = headerLine.split(",");
                
                // Process data rows
                String line;
                int recordId = 1;
                while ((line = reader.readLine()) != null) {
                    String[] values = line.split(",");
                    
                    txtBuilder.append("Record ").append(recordId++).append(":\n");
                    
                    for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                        String header = headers[i].trim();
                        String value = values[i].trim();
                        
                        txtBuilder.append(header).append(": ").append(value).append("\n");
                    }
                    
                    txtBuilder.append("\n");
                }
                
                Files.write(targetPath, txtBuilder.toString().getBytes());
                logger.info("CSV to TXT conversion completed successfully");
            }
            
            return outputFileName;
        }
        
        // JSON to CSV conversion
        else if ("json".equals(sourceFormat) && "csv".equals(targetFormat)) {
            logger.info("Performing JSON to CSV conversion");
            
            // For larger files, use Hadoop
            if (fileSize > 10 * 1024 * 1024) { // > 10MB
                logger.info("Large file detected, using Hadoop for processing");
                try {
                    conversionJob.runJob(
                            sourcePath.toString(),
                            targetPath.toString(),
                            sourceFormat.toLowerCase(),
                            targetFormat.toLowerCase()
                    );
                    logger.info("Hadoop conversion completed successfully");
                    return outputFileName;
                } catch (Exception e) {
                    logger.error("Error during Hadoop conversion", e);
                    // Fall back to direct conversion
                }
            }
            
            // Direct conversion for smaller files or as fallback
            String jsonContent = new String(Files.readAllBytes(sourcePath));
            
            try {
                // Fix the JSON
                jsonContent = JsonFixer.fixJson(jsonContent);
                
                // Parse the JSON
                JSONArray jsonArray = new JSONArray(jsonContent);
                
                // Get all possible keys from all objects
                Set<String> allKeys = new HashSet<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    Iterator<String> keys = jsonObject.keys();
                    while (keys.hasNext()) {
                        allKeys.add(keys.next());
                    }
                }
                
                // Create header row
                String[] headers = allKeys.toArray(new String[0]);
                StringBuilder csvBuilder = new StringBuilder();
                
                for (int i = 0; i < headers.length; i++) {
                    csvBuilder.append(headers[i]);
                    if (i < headers.length - 1) {
                        csvBuilder.append(",");
                    }
                }
                csvBuilder.append("\n");
                
                // Create data rows
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    
                    for (int j = 0; j < headers.length; j++) {
                        String key = headers[j];
                        if (jsonObject.has(key)) {
                            String value = jsonObject.get(key).toString();
                            // Escape commas in the value
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
                
                Files.write(targetPath, csvBuilder.toString().getBytes());
                logger.info("JSON to CSV conversion completed successfully");
            } catch (Exception e) {
                logger.error("Error parsing JSON", e);
                throw new RuntimeException("Error parsing JSON: " + e.getMessage());
            }
            
            return outputFileName;
        }
        
        // JSON to TXT conversion
        else if ("json".equals(sourceFormat) && "txt".equals(targetFormat)) {
            logger.info("Performing JSON to TXT conversion");
            
            String jsonContent = new String(Files.readAllBytes(sourcePath));
            
            try {
                // Fix the JSON
                jsonContent = JsonFixer.fixJson(jsonContent);
                
                // Parse the JSON
                JSONArray jsonArray = new JSONArray(jsonContent);
                
                // Create plain text representation
                StringBuilder txtBuilder = new StringBuilder();
                
                // Process each object
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    txtBuilder.append("Record ").append(i + 1).append(":\n");
                    
                    Iterator<String> keys = jsonObject.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        String value = jsonObject.get(key).toString();
                        txtBuilder.append(key).append(": ").append(value).append("\n");
                    }
                    
                    txtBuilder.append("\n");
                }
                
                Files.write(targetPath, txtBuilder.toString().getBytes());
                logger.info("JSON to TXT conversion completed successfully");
            } catch (Exception e) {
                logger.error("Error parsing JSON", e);
                throw new RuntimeException("Error parsing JSON: " + e.getMessage());
            }
            
            return outputFileName;
        }
        
        // JSON to XML conversion
        else if ("json".equals(sourceFormat) && "xml".equals(targetFormat)) {
            logger.info("Performing JSON to XML conversion");
            
            String jsonContent = new String(Files.readAllBytes(sourcePath));
            
            try {
                // Fix the JSON
                jsonContent = JsonFixer.fixJson(jsonContent);
                
                // Parse the JSON
                JSONArray jsonArray = new JSONArray(jsonContent);
                
                // Create XML document
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                Document doc = docBuilder.newDocument();
                
                // Create root element
                Element rootElement = doc.createElement("records");
                doc.appendChild(rootElement);
                
                // Process each object
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    Element recordElement = doc.createElement("record");
                    recordElement.setAttribute("id", String.valueOf(i + 1));
                    rootElement.appendChild(recordElement);
                    
                    Iterator<String> keys = jsonObject.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        String value = jsonObject.get(key).toString();
                        
                        // Create valid XML element name (replace spaces and special chars)
                        String elementName = key.replaceAll("[^a-zA-Z0-9]", "_");
                        if (elementName.isEmpty() || !Character.isLetter(elementName.charAt(0))) {
                            elementName = "field_" + elementName;
                        }
                        
                        Element fieldElement = doc.createElement(elementName);
                        fieldElement.setTextContent(value);
                        recordElement.appendChild(fieldElement);
                    }
                }
                
                // Write XML to file
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(targetPath.toFile());
                transformer.transform(source, result);
                
                logger.info("JSON to XML conversion completed successfully");
            } catch (ParserConfigurationException | TransformerException e) {
                logger.error("Error creating XML", e);
                throw new RuntimeException("Error creating XML: " + e.getMessage());
            } catch (Exception e) {
                logger.error("Error parsing JSON", e);
                throw new RuntimeException("Error parsing JSON: " + e.getMessage());
            }
            
            return outputFileName;
        }
        
        // If not handled by specific methods, try the Hadoop job
        logger.info("Running Hadoop conversion job");
        try {
            conversionJob.runJob(
                    sourcePath.toString(),
                    targetPath.toString(),
                    sourceFormat.toLowerCase(),
                    targetFormat.toLowerCase()
            );
            logger.info("Hadoop conversion completed successfully");
        } catch (Exception e) {
            logger.error("Error during conversion", e);
            throw e;
        }
        
        return outputFileName;
    }

    public Resource loadFileAsResource(String fileName) throws MalformedURLException {
        logger.info("Loading file as resource: {}", fileName);
        Path filePath = outputStorageLocation.resolve(fileName).normalize();
        logger.info("Looking for file at: {}", filePath.toAbsolutePath());
        
        if (!Files.exists(filePath)) {
            logger.error("File does not exist: {}", filePath);
            throw new RuntimeException("File not found: " + fileName);
        }
        
        Resource resource = new UrlResource(filePath.toUri());
        
        if (resource.exists()) {
            logger.info("Resource exists, returning");
            return resource;
        } else {
            logger.error("File not found: {}", fileName);
            throw new RuntimeException("File not found: " + fileName);
        }
    }
}

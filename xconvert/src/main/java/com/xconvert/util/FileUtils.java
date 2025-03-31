package com.xconvert.util;

import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {
    
    public static String getFileExtension(String fileName) {
        return FilenameUtils.getExtension(fileName);
    }
    
    public static String getFileNameWithoutExtension(String fileName) {
        return FilenameUtils.getBaseName(fileName);
    }
    
    public static void mergeFiles(String directory, String outputFile) throws IOException {
        File dir = new File(directory);
        File[] files = dir.listFiles((d, name) -> name.startsWith("part-"));
        
        if (files != null && files.length > 0) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                for (File file : files) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            writer.write(line);
                            writer.newLine();
                        }
                    }
                }
            }
        }
    }
}

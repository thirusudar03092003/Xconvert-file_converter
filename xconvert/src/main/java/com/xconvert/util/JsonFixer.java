package com.xconvert.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonFixer {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonFixer.class);
    
    public static String fixJson(String json) {
        logger.info("Attempting to fix JSON: {}", json.substring(0, Math.min(100, json.length())));
        
        // Trim whitespace
        json = json.trim();
        
        // If it's empty, return an empty array
        if (json.isEmpty()) {
            return "[]";
        }
        
        // Check if it starts with [ and ends with ]
        boolean startsWithBracket = json.startsWith("[");
        boolean endsWithBracket = json.endsWith("]");
        
        // If it's already a valid array, return it
        if (startsWithBracket && endsWithBracket) {
            return json;
        }
        
        // If it starts with { and ends with }, wrap it in an array
        if (json.startsWith("{") && json.endsWith("}")) {
            return "[" + json + "]";
        }
        
        // If it's a comma-separated list of objects, wrap it in an array
        if (json.startsWith("{") && !json.endsWith("}")) {
            return "[" + json + "]";
        }
        
        // If it doesn't start with [ or {, it might be an invalid format
        // Try to make it a string value in an object in an array
        if (!startsWithBracket && !json.startsWith("{")) {
            return "[{\"value\":\"" + escapeJsonString(json) + "\"}]";
        }
        
        // Default fallback - wrap in array brackets
        return "[" + json + "]";
    }
    
    private static String escapeJsonString(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '\"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}

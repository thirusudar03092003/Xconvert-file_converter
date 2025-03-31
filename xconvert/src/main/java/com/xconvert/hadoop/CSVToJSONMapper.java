package com.xconvert.hadoop;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.json.JSONObject;

import java.io.IOException;

public class CSVToJSONMapper extends Mapper<LongWritable, Text, Text, Text> {
    
    private boolean isHeader = true;
    private String[] headers;
    
    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        String line = value.toString();
        
        if (isHeader) {
            // Process header line
            headers = line.split(",");
            isHeader = false;
            return;
        }
        
        // Process data lines
        String[] fields = line.split(",");
        JSONObject jsonObject = new JSONObject();
        
        for (int i = 0; i < Math.min(headers.length, fields.length); i++) {
            jsonObject.put(headers[i].trim(), fields[i].trim());
        }
        
        // Output key is the row number, value is the JSON string
        context.write(new Text(String.valueOf(key.get())), new Text(jsonObject.toString()));
    }
}

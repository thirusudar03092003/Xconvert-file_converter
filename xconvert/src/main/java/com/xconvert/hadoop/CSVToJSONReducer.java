package com.xconvert.hadoop;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.json.JSONArray;

import java.io.IOException;

public class CSVToJSONReducer extends Reducer<Text, Text, Text, Text> {
    
    private JSONArray jsonArray = new JSONArray();
    
    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        // Simply pass through the JSON objects
        for (Text value : values) {
            context.write(new Text(""), value);
        }
    }
    
    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        // Write the final JSON array
        context.write(new Text(""), new Text(jsonArray.toString()));
    }
}

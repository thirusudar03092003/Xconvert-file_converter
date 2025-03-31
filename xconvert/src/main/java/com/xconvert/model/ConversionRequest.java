package com.xconvert.model;

public class ConversionRequest {
    private String sourceFormat;
    private String targetFormat;
    private String fileName;

    public ConversionRequest() {
    }

    public ConversionRequest(String sourceFormat, String targetFormat, String fileName) {
        this.sourceFormat = sourceFormat;
        this.targetFormat = targetFormat;
        this.fileName = fileName;
    }

    public String getSourceFormat() {
        return sourceFormat;
    }

    public void setSourceFormat(String sourceFormat) {
        this.sourceFormat = sourceFormat;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(String targetFormat) {
        this.targetFormat = targetFormat;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "ConversionRequest{" +
                "sourceFormat='" + sourceFormat + '\'' +
                ", targetFormat='" + targetFormat + '\'' +
                ", fileName='" + fileName + '\'' +
                '}';
    }
}

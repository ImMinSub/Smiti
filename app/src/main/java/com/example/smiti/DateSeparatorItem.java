package com.example.smiti;

public class DateSeparatorItem {
    private String dateText;
    private long timestamp;
    
    public DateSeparatorItem(String dateText, long timestamp) {
        this.dateText = dateText;
        this.timestamp = timestamp;
    }
    
    public String getDateText() {
        return dateText;
    }
    
    public void setDateText(String dateText) {
        this.dateText = dateText;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
} 

package com.s23010749.nutriscan;

public class ScanHistoryItem {
    public long id;
    public String foodName, healthStatus, nutrientsJson, tips, date, location;

    public ScanHistoryItem(long id, String foodName, String healthStatus, String nutrientsJson, String tips, String date, String location) {
        this.id = id;
        this.foodName = foodName;
        this.healthStatus = healthStatus;
        this.nutrientsJson = nutrientsJson;
        this.tips = tips;
        this.date = date;
        this.location = location;
    }
}
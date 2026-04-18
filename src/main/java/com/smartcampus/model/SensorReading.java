package com.smartcampus.model;

/**
 * Represents a single timestamped data point recorded by a Sensor.
 * Readings are immutable historical records — they are never updated after creation.
 */
public class SensorReading {

    private String id;        // Unique reading event ID (UUID)
    private long timestamp;   // Epoch time in milliseconds when reading was captured
    private double value;     // The actual metric value recorded by the hardware

    // --- Constructors ---

    public SensorReading() {}

    public SensorReading(String id, long timestamp, double value) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
    }

    // --- Getters & Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
}
package com.smartcampus.data;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton in-memory data store.
 *
 * WHY ConcurrentHashMap?
 * JAX-RS creates a new resource class instance per request (request-scoped lifecycle).
 * Multiple requests can therefore run concurrently on separate threads.
 * ConcurrentHashMap provides thread-safe read/write operations without explicit
 * synchronization, preventing race conditions where two threads could corrupt
 * the same data structure simultaneously.
 */
public class DataStore {

    // --- Singleton Pattern ---
    private static final DataStore INSTANCE = new DataStore();

    public static DataStore getInstance() {
        return INSTANCE;
    }

    // --- Private constructor prevents external instantiation ---
    private DataStore() {
        seedData(); // Pre-load sample data so the API is not empty on first run
    }

    // --- In-Memory Storage ---
    // Key = resource ID, Value = resource object
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();

    // Readings are stored per sensor: sensorId -> list of readings
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    // --- Public Accessors ---

    public Map<String, Room> getRooms() { return rooms; }
    public Map<String, Sensor> getSensors() { return sensors; }
    public Map<String, List<SensorReading>> getReadings() { return readings; }

    /**
     * Returns the readings list for a given sensor.
     * Creates an empty list if none exists yet (thread-safe).
     */
    public List<SensorReading> getReadingsForSensor(String sensorId) {
        return readings.computeIfAbsent(sensorId, k -> new ArrayList<>());
    }

    // --- Seed Data ---

    private void seedData() {
        // Seed two rooms
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room r2 = new Room("LAB-101", "Computer Science Lab", 30);

        // Seed two sensors
        Sensor s1 = new Sensor("TEMP-001", "Temperature", "MAINTENANCE", 21.5, "LIB-301");
        Sensor s2 = new Sensor("CO2-001", "CO2", "ACTIVE", 412.0, "LAB-101");

        // Link sensors to rooms
        r1.getSensorIds().add(s1.getId());
        r2.getSensorIds().add(s2.getId());

        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);
        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
    }
}
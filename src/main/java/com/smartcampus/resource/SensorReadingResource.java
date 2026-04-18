package com.smartcampus.resource;

import com.smartcampus.data.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.ApiError;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.UUID;

public class SensorReadingResource {

    private final DataStore store = DataStore.getInstance();
    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllReadings() {
        List<SensorReading> history = store.getReadingsForSensor(sensorId);
        return Response.ok(history).build();
    }

    @GET
    @Path("/{readingId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReadingById(@PathParam("readingId") String readingId) {
        List<SensorReading> history = store.getReadingsForSensor(sensorId);

        return history.stream()
            .filter(r -> r.getId().equals(readingId))
            .findFirst()
            .map(r -> Response.ok(r).build())
            .orElse(
                Response.status(Response.Status.NOT_FOUND)
                        .entity(new ApiError(404, "Not Found",
                            "Reading '" + readingId + "' not found for sensor '"
                            + sensorId + "'."))
                        .type(MediaType.APPLICATION_JSON)
                        .build()
            );
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading) {

        Sensor sensor = store.getSensors().get(sensorId);

        // Block readings for sensors under maintenance
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId);
        }

        if (reading == null) {
            return Response.status(400)
                    .entity(new ApiError(400, "Bad Request",
                            "Request body is required."))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // Auto-generate id if not provided
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }

        // Auto-generate timestamp if not provided
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Save the reading
        store.getReadingsForSensor(sensorId).add(reading);

        // SIDE EFFECT: update parent sensor's currentValue
        sensor.setCurrentValue(reading.getValue());

        URI location = UriBuilder
            .fromPath("/api/v1/sensors/{sensorId}/readings/{readingId}")
            .build(sensorId, reading.getId());

        return Response.created(location).entity(reading).build();
    }

    @DELETE
    @Path("/{readingId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteReading(@PathParam("readingId") String readingId) {
        List<SensorReading> history = store.getReadingsForSensor(sensorId);
        boolean removed = history.removeIf(r -> r.getId().equals(readingId));

        if (!removed) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiError(404, "Not Found",
                        "Reading '" + readingId + "' not found for sensor '"
                        + sensorId + "'."))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        return Response.noContent().build();
    }
}
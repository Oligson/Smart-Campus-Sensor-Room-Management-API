package com.smartcampus.resource;

import com.smartcampus.data.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.exception.SensorNotFoundException;
import com.smartcampus.model.ApiError;
import com.smartcampus.model.Sensor;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> result = store.getSensors().values().stream()
            .filter(s -> type == null || s.getType().equalsIgnoreCase(type))
            .collect(Collectors.toList());
        return Response.ok(result).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {

        if (sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(400)
                    .entity(new ApiError(400, "Bad Request",
                            "Sensor 'id' field is required."))
                    .build();
        }
        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
            return Response.status(400)
                    .entity(new ApiError(400, "Bad Request",
                            "Sensor 'roomId' field is required."))
                    .build();
        }
        if (store.getSensors().containsKey(sensor.getId())) {
            return Response.status(409)
                    .entity(new ApiError(409, "Conflict",
                            "A sensor with ID '" + sensor.getId() + "' already exists."))
                    .build();
        }
        if (!store.getRooms().containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException("roomId", sensor.getRoomId());
        }
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        store.getSensors().put(sensor.getId(), sensor);
        store.getRooms()
             .get(sensor.getRoomId())
             .getSensorIds()
             .add(sensor.getId());

        URI location = UriBuilder.fromPath("/api/v1/sensors/{id}")
                                 .build(sensor.getId());

        return Response.created(location).entity(sensor).build();
    }

    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            throw new SensorNotFoundException(sensorId);
        }
        return Response.ok(sensor).build();
    }

    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            throw new SensorNotFoundException(sensorId);
        }

        String parentRoomId = sensor.getRoomId();
        if (parentRoomId != null && store.getRooms().containsKey(parentRoomId)) {
            store.getRooms()
                 .get(parentRoomId)
                 .getSensorIds()
                 .remove(sensorId);
        }

        store.getSensors().remove(sensorId);
        return Response.noContent().build();
    }

    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(
            @PathParam("sensorId") String sensorId) {
        if (!store.getSensors().containsKey(sensorId)) {
            throw new SensorNotFoundException(sensorId);
        }
        return new SensorReadingResource(sensorId);
    }
}
package com.smartcampus.resource;

import com.smartcampus.data.DataStore;
import com.smartcampus.exception.RoomNotFoundException;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.ApiError;
import com.smartcampus.model.Room;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    @GET
    public Response getAllRooms() {
        List<Room> allRooms = new ArrayList<>(store.getRooms().values());
        return Response.ok(allRooms).build();
    }

    @POST
    public Response createRoom(Room room) {

        if (room.getId() == null || room.getId().isBlank()) {
            return Response.status(400)
                    .entity(new ApiError(400, "Bad Request",
                            "Room 'id' field is required."))
                    .build();
        }
        if (room.getName() == null || room.getName().isBlank()) {
            return Response.status(400)
                    .entity(new ApiError(400, "Bad Request",
                            "Room 'name' field is required."))
                    .build();
        }
        if (room.getCapacity() <= 0) {
            return Response.status(400)
                    .entity(new ApiError(400, "Bad Request",
                            "Room 'capacity' must be greater than zero."))
                    .build();
        }
        if (store.getRooms().containsKey(room.getId())) {
            return Response.status(409)
                    .entity(new ApiError(409, "Conflict",
                            "A room with ID '" + room.getId() + "' already exists."))
                    .build();
        }
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }

        store.getRooms().put(room.getId(), room);

        URI location = UriBuilder.fromPath("/api/v1/rooms/{id}")
                                 .build(room.getId());

        return Response.created(location).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            throw new RoomNotFoundException(roomId);
        }
        return Response.ok(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);

        if (room == null) {
            throw new RoomNotFoundException(roomId);
        }
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId);
        }

        store.getRooms().remove(roomId);
        return Response.noContent().build();
    }
}
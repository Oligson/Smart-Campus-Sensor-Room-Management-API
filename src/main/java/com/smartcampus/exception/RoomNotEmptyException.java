package com.smartcampus.exception;

public class RoomNotEmptyException extends RuntimeException {

    private final String roomId;

    public RoomNotEmptyException(String roomId) {
        super("Room '" + roomId + "' still has active sensors assigned. "
            + "Relocate or delete all sensors before decommissioning this room.");
        this.roomId = roomId;
    }

    public String getRoomId() { return roomId; }
}
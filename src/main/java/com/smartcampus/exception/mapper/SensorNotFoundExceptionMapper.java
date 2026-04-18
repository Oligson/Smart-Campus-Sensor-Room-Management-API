package com.smartcampus.exception.mapper;

import com.smartcampus.exception.SensorNotFoundException;
import com.smartcampus.model.ApiError;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class SensorNotFoundExceptionMapper 
        implements ExceptionMapper<SensorNotFoundException> {

    @Override
    public Response toResponse(SensorNotFoundException ex) {
        ApiError error = new ApiError(
            Response.Status.NOT_FOUND.getStatusCode(),
            "Not Found",
            ex.getMessage()
        );
        return Response.status(Response.Status.NOT_FOUND)
                       .entity(error)
                       .type(MediaType.APPLICATION_JSON)
                       .build();
    }
}
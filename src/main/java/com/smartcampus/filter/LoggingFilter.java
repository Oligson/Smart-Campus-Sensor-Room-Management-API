package com.smartcampus.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

@Provider
public class LoggingFilter 
        implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER =
        Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) 
            throws IOException {
        LOGGER.info(String.format(
            "--> %s %s",
            requestContext.getMethod(),
            requestContext.getUriInfo().getRequestUri().toString()
        ));
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) 
            throws IOException {
        LOGGER.info(String.format(
            "<-- %d %s",
            responseContext.getStatus(),
            responseContext.getStatusInfo().getReasonPhrase()
        ));
    }
}
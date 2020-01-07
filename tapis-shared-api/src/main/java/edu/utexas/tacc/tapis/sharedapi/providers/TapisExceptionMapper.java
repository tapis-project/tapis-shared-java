package edu.utexas.tacc.tapis.sharedapi.providers;

import edu.utexas.tacc.tapis.sharedapi.responses.TapisResponse;

import javax.annotation.Priority;
import javax.validation.ValidationException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;


/**
 * This will catch any generic errors and return a 4 part Tapis response. This needs to be registered
 * in the jersey application like register(TapisExceptionMapper.class)
 *
 */
@Provider
public class TapisExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception){

        TapisResponse resp = TapisResponse.createErrorResponse(exception.getMessage());

         if (exception instanceof NotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(resp).build();
        } else if (exception instanceof NotAuthorizedException ) {
             return Response.status(Response.Status.FORBIDDEN)
                     .type(MediaType.APPLICATION_JSON)
                     .entity(resp).build();
        } else if (exception instanceof BadRequestException) {
             return Response.status(Response.Status.BAD_REQUEST)
                     .type(MediaType.APPLICATION_JSON)
                     .entity(resp).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(resp).build();
        }

    }
}
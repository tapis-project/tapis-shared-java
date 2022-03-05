package edu.utexas.tacc.tapis.sharedapi.providers;

import javax.validation.ValidationException;

import edu.utexas.tacc.tapis.sharedapi.responses.TapisResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;


/**
 * This will catch any validation errors and return a standard Tapis response. This needs to be registered
 * in the jersey application like register(ValidationExceptionMapper.class). Since jersey 2.x uses its own
 * ValidationExceptionMapper, we have to basically override that specifically. Other exceptions can get caught by the
 * TapisExceptionMapper.
 *
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException>
{

    @Override
    public Response toResponse(ValidationException exception){
        //TODO: I think we can add in a list of all the validation errors in here?
        TapisResponse<String> resp = TapisResponse.createErrorResponse(exception.getMessage());

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(resp).build();
    }
}
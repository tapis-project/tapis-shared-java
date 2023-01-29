package edu.utexas.tacc.tapis.sharedapi.providers;

import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.sharedapi.responses.RespBasic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

/*
 * Class used to map various exception types to a RespBasic.
 * Support for mapping:
 *    NotFound, NotAuthorized, Forbidden, BadRequest and WebApplication.
 * Other exception types result in an UNCAUGHT_EXCEPTION error and generation of an INTERNAL_SERVER_ERROR response.
 */
public class ApiExceptionMapper implements ExceptionMapper<Exception>
{
  private static final Logger log = LoggerFactory.getLogger(ApiExceptionMapper.class);

  @Override
  public Response toResponse(Exception exception)
  {
    // Determine the response status
    Response.Status status = INTERNAL_SERVER_ERROR;
    if (exception instanceof NotFoundException) status = NOT_FOUND;
    else if (exception instanceof NotAuthorizedException) status = UNAUTHORIZED;
    else if (exception instanceof ForbiddenException) status = FORBIDDEN;
    else if (exception instanceof BadRequestException) status = BAD_REQUEST;
    else if (exception instanceof WebApplicationException) status = INTERNAL_SERVER_ERROR;
    else if (exception instanceof TapisClientException tce) status = Response.Status.valueOf(tce.getStatus());
    else log.error("UNCAUGHT_EXCEPTION", exception);
    // Construct a basic response
    var resp = new RespBasic();
    resp.status = status.toString();
    resp.message = exception.getMessage();
    resp.version = TapisUtils.getTapisVersion();
    resp.commit  = TapisUtils.getGitCommit();
    resp.build   = TapisUtils.getBuildTime();
    resp.metadata = null;
    // Create pretty-printed response string
    String rStr = TapisGsonUtils.getGson(true).toJson(resp);
    return Response.status(status).type(MediaType.APPLICATION_JSON).entity(rStr).build();
  }
}

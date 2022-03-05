package edu.utexas.tacc.tapis.sharedapi.utils;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.commons.lang3.StringUtils;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.runtime.TapisRuntimeException;
import edu.utexas.tacc.tapis.shared.security.TenantManager;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;
import edu.utexas.tacc.tapis.sharedapi.responses.RespBasic;


import java.util.HashMap;

/**
 * This class provides a way to generate JSON responses to HTTP
 * calls that is amenable to openapi code generation.
 * 
 * @author rcardone
 */
public class TapisRestUtils 
{
    /* **************************************************************************** */
    /*                                    Enums                                     */
    /* **************************************************************************** */
    // Possible status field values.
    public enum RESPONSE_STATUS {success, error}

  /* **************************************************************************** */
  /*                                    Fields                                    */
  /* **************************************************************************** */
  // Mapping of exception class names to HTTP status response codes.  The names are
  // those returned from getClass().getName(). See the initialization method for a
  // note concerning maintenance.
  private static final HashMap<String, Response.Status> _exceptionStatuses = initExceptionStatuses();

  /* **************************************************************************** */
    /*                                Public Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* createSuccessResponse:                                                       */
    /* ---------------------------------------------------------------------------- */
    /** Return the json string that represent the response to a REST call that succeeds
     * and returns a result object.
     * 
     * @param message the application level error message to be included in the response
     * @param prettyPrint true for multi-line formatting, false for compact formatting
     * @param resp a response object to be converted to json
     * @return the json response string
     */
    public static String createSuccessResponse(String message, boolean prettyPrint, RespAbstract resp)
    {
        // Fill in the base fields.
        resp.status = RESPONSE_STATUS.success.name();
        resp.message = message;
        resp.version = TapisUtils.getTapisVersion();
        return TapisGsonUtils.getGson(prettyPrint).toJson(resp);
    }
    
    /* ---------------------------------------------------------------------------- */
    /* createSuccessResponse:                                                       */
    /* ---------------------------------------------------------------------------- */
    /** Return the json string that represent the response to a REST call that succeeds.
     * 
     * @param message the application level error message to be included in the response
     * @param prettyPrint true for multi-line formating, false for compact formatting
     * @return the json response string
     */
    public static String createSuccessResponse(String message, boolean prettyPrint)
    {
        // Fill in the base fields.
        RespBasic resp = new RespBasic();
        resp.status = RESPONSE_STATUS.success.name();
        resp.message = message;
        resp.version = TapisUtils.getTapisVersion();
        return TapisGsonUtils.getGson(prettyPrint).toJson(resp);
    }
    
    /* ---------------------------------------------------------------------------- */
    /* createErrorResponse:                                                         */
    /* ---------------------------------------------------------------------------- */
    /** Return the json string that represent the response to a REST call that 
     * experienced an error and returns a result object.
     * 
     * @param message the application level error message to be included in the response
     * @param prettyPrint true for multi-line formating, false for compact formatting
     * @param resp a response object to be converted to json
     * @return the json response string
     */
    public static String createErrorResponse(String message, boolean prettyPrint, RespAbstract resp)
    {
        // Fill in the base fields.
        resp.status = RESPONSE_STATUS.error.name();
        resp.message = message;
        resp.version = TapisUtils.getTapisVersion();
        return TapisGsonUtils.getGson(prettyPrint).toJson(resp);
    }
    
    /* ---------------------------------------------------------------------------- */
    /* createErrorResponse:                                                         */
    /* ---------------------------------------------------------------------------- */
    /** Return the json string that represent the response to a REST call that 
     * experienced an error.
     * 
     * @param message the application level error message to be included in the response
     * @param prettyPrint true for multi-line formating, false for compact formatting
     * @return the json response string
     */
    public static String createErrorResponse(String message, boolean prettyPrint)
    {
        // Fill in the base fields.
        RespBasic resp = new RespBasic();
        resp.status = RESPONSE_STATUS.error.name();
        resp.message = message;
        resp.version = TapisUtils.getTapisVersion();
        return TapisGsonUtils.getGson(prettyPrint).toJson(resp);
    }
    
    /* ---------------------------------------------------------------------------- */
    /* checkJWTSubjectFormat:                                                       */
    /* ---------------------------------------------------------------------------- */
    /** Return true if the subject is non-null and of the form user@tenant, false 
     * otherwise. 
     * 
     * @param subject a subject in the form user@tenant
     * @return true is valid, false otherwise
     */
    public static boolean checkJWTSubjectFormat(String subject)
    {
        // Null or empty string don't cut it.
        if (StringUtils.isBlank(subject)) return false;
        
        // Test for non-whitespace characters on both sides of the @ sign.
        String trimmedSubject = subject.trim();
        int index = trimmedSubject.indexOf("@");
        if (index < 1 || (index >= trimmedSubject.length() - 1)) return false;
        
        // Correct format.
        return true;
    }
    
    /* ---------------------------------------------------------------------- */
    /* isAllowedTenant:                                                       */
    /* ---------------------------------------------------------------------- */
    /** Determine if the tenant specified in the jwt's tapis/tenant_id claim is
     * allowed to execute on behalf of the target tenant specified in the 
     * X-Tapis-Tenant header or the delegation_sub claim.  Neither parameter 
     * can be null.
     * 
     * @param jwtTenantId the tenant assigned in the jwt tapis/tenant_id claim
     * @param newTenantId the tenant assigned in the X-Tapis-Tenant header
     * @return true if the jwt tenant can execute on behalf of the new tenant,
     *         false otherwise
     * @throws TapisException 
     * @throws TapisRuntimeException 
     */
    public static boolean isAllowedTenant(String jwtTenantId, String newTenantId) 
     throws TapisRuntimeException, TapisException
    {
        // This method will return a non-null tenant or throw an exception.
        return  TenantManager.getInstance().allowTenantId(jwtTenantId, newTenantId);
    }

  /* ---------------------------------------------------------------------------- */
  /* getStatus:                                                                   */
  /* ---------------------------------------------------------------------------- */
  /** Return the HTTP status code in a response based on the exception generated by
   * a request.  If an exception type is not recognized INTERNAL_SERVER_ERROR
   * is returned.
   *
   * Note that keeping this code outside of the exception classes themselves keep
   * HTTP knowledge outside the non-api code.
   *
   * @param t the exception caused during the processing of an HTTP request
   * @return the status associated with the exception type or INTERNAL_SERVER_ERROR
   */
  public static Response.Status getStatus(Throwable t)
  {
    return getStatus(t, Status.INTERNAL_SERVER_ERROR);
  }

  /* ---------------------------------------------------------------------------- */
  /* getStatus:                                                                   */
  /* ---------------------------------------------------------------------------- */
  /** Return the HTTP status code in a response based on the exception generated by
   * a request.  If an exception type is not recognized the default status parameter
   * is returned.
   *
   * Note that keeping this code outside of the exception classes themselves keep
   * HTTP knowledge outside the non-api code.
   *
   * @param t the exception caused during the processing of an HTTP request
   * @param defaultStatus the status returned by default for unrecognized exceptions
   * @return the status associated with the exception type or the default status
   */
  public static Status getStatus(Throwable t, Status defaultStatus)
  {
    // This should never happen.
    if ((t == null) || (defaultStatus == null)) return Status.INTERNAL_SERVER_ERROR;

    // See if this exception has been mapped.
    Status status = _exceptionStatuses.get(t.getClass().getName());
    if (status != null) return status;
    else return defaultStatus;
  }

  /* **************************************************************************** */
  /*                                Private Methods                               */
  /* **************************************************************************** */
  /* ---------------------------------------------------------------------------- */
  /* initExceptionStatuses:                                                       */
  /* ---------------------------------------------------------------------------- */
  /** Create the mapping used by getStatus() to select a HTTP status codes given an
   * exception.  The names are those returned from getClass().getName().
   *
   * MAINTENANCE NOTE:  If an exception's name or package changes and it has an
   *                    entry in the map below, then that entry must also change.
   *
   * DESIGN NOTE:  Other designs considered for mapping exceptions to HTTP codes
   *  are listed below.  Maybe there's a better way to do this, but what we have
   *  now is clear and fast despite the extra maintenance on rare occasions.
   *
   *  A) Carry the HTTP code in the exception.
   *      - Requires mix HTTP/REST notions in library code that is otherwise request
   *        oblivious.  Doesn't handle non-Tapis exception well.
   *  B) Use exception type as map key.
   *      - Requires putting all Tapis exceptions in the shared library.
   *  C) Compare strings using an IF or SWITCH statement in getStatus() rather than a map.
   *      - Same maintenance issue as current map approach except slower.
   *  D) Compare class objects (i.e., addresses) using IF or SWITCH in getStatus().
   *      - Same issues as A).
   *
   * @return the exception to status mapping
   */
  private static HashMap<String,Status> initExceptionStatuses()
  {
    // Set the capacity to be about twice the number of entries to avoid rehashing.
    HashMap<String,Status> map = new HashMap<>(23);
    map.put("edu.utexas.tacc.tapis.shared.exceptions.TapisException", Status.INTERNAL_SERVER_ERROR);
    map.put("edu.utexas.tacc.tapis.shared.exceptions.TapisRuntimeException", Status.INTERNAL_SERVER_ERROR);

    map.put("edu.utexas.tacc.tapis.shared.exceptions.TapisJDBCException", Status.INTERNAL_SERVER_ERROR);
    map.put("edu.utexas.tacc.tapis.shared.exceptions.TapisJSONException", Status.BAD_REQUEST);
    map.put("edu.utexas.tacc.tapis.shared.exceptions.TapisUUIDException", Status.INTERNAL_SERVER_ERROR);

    map.put("edu.utexas.tacc.tapis.jobs.exceptions.JobException", Status.INTERNAL_SERVER_ERROR);
    map.put("edu.utexas.tacc.tapis.jobs.exceptions.JobQueueException", Status.INTERNAL_SERVER_ERROR);
    map.put("edu.utexas.tacc.tapis.jobs.exceptions.JobQueueFilterException", Status.BAD_REQUEST);
    map.put("edu.utexas.tacc.tapis.jobs.exceptions.JobQueuePriorityException", Status.BAD_REQUEST);
    map.put("edu.utexas.tacc.tapis.jobs.exceptions.JobInputException", Status.BAD_REQUEST);

    return map;
  }
}

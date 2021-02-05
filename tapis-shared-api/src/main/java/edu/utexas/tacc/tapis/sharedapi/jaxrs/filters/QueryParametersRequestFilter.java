package edu.utexas.tacc.tapis.sharedapi.jaxrs.filters;

import edu.utexas.tacc.tapis.search.SearchUtils;
import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadLocal;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.List;

import static edu.utexas.tacc.tapis.search.SearchUtils.*;

/*
 *  jax-rs filter to intercept various query parameters and set values in the thread context.
 *  Parameters:
 *    pretty - Boolean indicating if response should be pretty printed. Default is false.
 *    search - String indicating search conditions to use when retrieving results
 *    limit - Integer indicating maximum number of results to be included, -1 for unlimited
 *    sortBy - e.g. sortBy=owner(asc), sortBy=created(desc)
 *    skip - number of results to skip
 *    startAfter - e.g. systems?limit=10&sortBy=id(asc)&startAfter=101
 *    computeTotal - Boolean indicating if total count should be computed. Default is false.
 *
 *  NOTE: Process "pretty" here because it is a parameter for all endpoints and
 *        is not needed for the java client or the back-end (tapis-systemslib)
 */
@Provider
@Priority(TapisConstants.JAXRS_FILTER_PRIORITY_AFTER_AUTHENTICATION)
public class QueryParametersRequestFilter implements ContainerRequestFilter
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Tracing.
  private static final Logger _log = LoggerFactory.getLogger(QueryParametersRequestFilter.class);

  // Query parameter names
  private static final String PARM_PRETTY = "pretty";
  private static final String PARM_FILTER = "fields";
  private static final String PARM_SEARCH = "search";
  private static final String PARM_LIMIT = "limit";
  private static final String PARM_SORTBY = "sortBy";
  private static final String PARM_SKIP = "skip";
  private static final String PARM_STARTAFTER = "startAfter";
  private static final String PARM_COMPUTETOTAL = "computeTotal";

  /* ********************************************************************** */
  /*                            Public Methods                              */
  /* ********************************************************************** */
  /* ---------------------------------------------------------------------- */
  /* filter:                                                                */
  /* ---------------------------------------------------------------------- */
  @Override
  public void filter(ContainerRequestContext requestContext)
  {
    // Tracing.
    if (_log.isTraceEnabled())
      _log.trace("Executing JAX-RX request filter: " + this.getClass().getSimpleName() + ".");

    // Get thread context
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();

    // Set default sort and paginate options
    threadContext.setLimit(DEFAULT_LIMIT);
    threadContext.setSortBy(DEFAULT_SORTBY);
    threadContext.setSortByDirection(DEFAULT_SORTBY_DIRECTION);
    threadContext.setSkip(DEFAULT_SKIP);
    threadContext.setStartAfter(DEFAULT_STARTAFTER);
    threadContext.setComputeTotal(DEFAULT_COMPUTETOTAL);

    // Retrieve all query parameters. If none we are done.
    MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters();
    if (queryParameters == null || queryParameters.isEmpty()) return;

    // Look for and extract pretty print query parameter.
    // Common checks for query parameters
    if (invalidParm(threadContext, requestContext, PARM_PRETTY)) { return; }
    String parmValuePretty = getQueryParm(queryParameters, PARM_PRETTY);
    if (!StringUtils.isBlank(parmValuePretty))
    {
      // Provided parameter is valid. Set as boolean
      if (!"true".equalsIgnoreCase(parmValuePretty) && !"false".equalsIgnoreCase(parmValuePretty))
      {
        String msg = MsgUtils.getMsg("TAPIS_QUERY_PARAM_NOTBOOL", threadContext.getJwtTenantId(), threadContext.getJwtUser(),
                threadContext.getOboTenantId(), threadContext.getOboUser(), PARM_PRETTY, parmValuePretty);
        _log.warn(msg);
      }
      else
      {
        threadContext.setPrettyPrint(Boolean.parseBoolean(parmValuePretty));
      }
    }

    // Look for and extract computeTotal query parameter.
    // Common checks for query parameters
    if (invalidParm(threadContext, requestContext, PARM_COMPUTETOTAL)) { return; }
    String parmValueComputeTotal = getQueryParm(queryParameters, PARM_COMPUTETOTAL);
    if (!StringUtils.isBlank(parmValueComputeTotal))
    {
      // Provided parameter is valid. Set as boolean
      if (!"true".equalsIgnoreCase(parmValueComputeTotal) && !"false".equalsIgnoreCase(parmValueComputeTotal))
      {
        String msg = MsgUtils.getMsg("TAPIS_QUERY_PARAM_NOTBOOL", threadContext.getJwtTenantId(), threadContext.getJwtUser(),
                threadContext.getOboTenantId(), threadContext.getOboUser(), PARM_COMPUTETOTAL, parmValueComputeTotal);
        _log.warn(msg);
      }
      else
      {
        threadContext.setComputeTotal(Boolean.parseBoolean(parmValueComputeTotal));
      }
    }

    // Look for and extract filter query parameter.
    // This parameter is used to select which result fields are included in a response.
    if (invalidParm(threadContext, requestContext, PARM_FILTER)) { return; }
    String parmValueFields = getQueryParm(queryParameters, PARM_FILTER);
    // Extract and validate the fields.
    try
    {
      List<String> filterList = SearchUtils.getValueList(parmValueFields);
      threadContext.setFilterList(filterList);
    }
    catch (Exception e)
    {
      String msg = MsgUtils.getMsg("FILTER_LIST_ERROR", threadContext.getJwtTenantId(), threadContext.getJwtUser(),
              threadContext.getOboTenantId(), threadContext.getOboUser(), e.getMessage());
      _log.error(msg, e);
      requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).entity(msg).build());
      return;
    }

    // Look for and extract search query parameter.
    if (invalidParm(threadContext, requestContext, PARM_SEARCH)) { return; }
    String parmValueSearch = getQueryParm(queryParameters, PARM_SEARCH);
    // Extract the search conditions and validate their form. Back end will handle translating LIKE wildcard
    //   characters (* and !) and dealing with special characters in values.
    try
    {
      List<String> searchList = SearchUtils.extractAndValidateSearchList(parmValueSearch);
      threadContext.setSearchList(searchList);
    }
    catch (Exception e)
    {
      String msg = MsgUtils.getMsg("SEARCH_LIST_ERROR", threadContext.getJwtTenantId(), threadContext.getJwtUser(),
                                   threadContext.getOboTenantId(), threadContext.getOboUser(), e.getMessage());
      _log.error(msg, e);
      requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).entity(msg).build());
      return;
    }

    // Look for and extract limit query parameter.
    if (invalidParm(threadContext, requestContext, PARM_LIMIT)) { return; }
    String parmValueLimit = getQueryParm(queryParameters, PARM_LIMIT);
    if (!StringUtils.isBlank(parmValueLimit))
    {
      int limit = DEFAULT_LIMIT;
      // Check that it is an integer
      try { limit = Integer.parseInt(parmValueLimit); }
      catch (NumberFormatException e)
      {
        String msg = MsgUtils.getMsg("TAPIS_QUERY_PARAM_NOTINT", threadContext.getJwtTenantId(), threadContext.getJwtUser(),
                threadContext.getOboTenantId(), threadContext.getOboUser(), PARM_LIMIT, parmValueLimit);
        _log.error(msg);
        requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).entity(msg).build());
        return;
      }
      threadContext.setLimit(limit);
    }

    // Look for and extract sortBy query parameter.
    if (invalidParm(threadContext, requestContext, PARM_SORTBY)) { return; }
    String parmValueSortBy = getQueryParm(queryParameters, PARM_SORTBY);
    if (!StringUtils.isBlank(parmValueSortBy))
    {
      // Validate and process sortBy which must be in the form <col_name>(<dir>)
      //   where (<dir>) is optional and <dir> = "asc" or "desc"
      String errMsg = SearchUtils.checkSortByQueryParam(parmValueSortBy);
      if (errMsg != null)
      {
        String msg = MsgUtils.getMsg("TAPIS_QUERY_PARAM_SORTBY_ERROR", threadContext.getJwtTenantId(), threadContext.getJwtUser(),
                                     threadContext.getOboTenantId(), threadContext.getOboUser(), errMsg);
        _log.error(msg);
        requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).entity(msg).build());
        return;
      }
      threadContext.setSortBy(SearchUtils.getSortByColumn(parmValueSortBy));
      threadContext.setSortByDirection(SearchUtils.getSortByDirection(parmValueSortBy));
    }

    // Look for and extract skip query parameter.
    if (invalidParm(threadContext, requestContext, PARM_SKIP)) { return; }
    String parmValueSkip = getQueryParm(queryParameters, PARM_SKIP);
    if (!StringUtils.isBlank(parmValueSkip))
    {
      int skip;
      // Check that it is an integer
      try { skip = Integer.parseInt(parmValueSkip); }
      catch (NumberFormatException e)
      {
        String msg = MsgUtils.getMsg("TAPIS_QUERY_PARAM_NOTINT", threadContext.getJwtTenantId(), threadContext.getJwtUser(),
                threadContext.getOboTenantId(), threadContext.getOboUser(), PARM_SKIP, parmValueSkip);
        _log.error(msg);
        requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).entity(msg).build());
        return;
      }
      threadContext.setSkip(skip);
    }

    // Look for and extract startAfter query parameter.
    if (invalidParm(threadContext, requestContext, PARM_STARTAFTER)) { return; }
    String parmValueStartAfter = getQueryParm(queryParameters, PARM_STARTAFTER);
    if (!StringUtils.isBlank(parmValueStartAfter)) threadContext.setStartAfter(parmValueStartAfter);

    // Check constraints
    // Specifying startAfter without sortBy is an invalid combination
    if (!StringUtils.isBlank(threadContext.getStartAfter()) && StringUtils.isBlank(threadContext.getSortBy()))
    {
      String msg = MsgUtils.getMsg("TAPIS_QUERY_PARAM_INVALID_PAIR1", threadContext.getJwtTenantId(), threadContext.getJwtUser(),
              threadContext.getOboTenantId(), threadContext.getOboUser(), PARM_STARTAFTER, PARM_SORTBY);
      _log.error(msg);
      requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).entity(msg).build());
      return;
    }
    // Specifying startAfter and skip is an invalid combination
    if (!StringUtils.isBlank(threadContext.getStartAfter()) && !StringUtils.isBlank(parmValueSkip))
    {
      String msg = MsgUtils.getMsg("TAPIS_QUERY_PARAM_INVALID_PAIR2", threadContext.getJwtTenantId(), threadContext.getJwtUser(),
              threadContext.getOboTenantId(), threadContext.getOboUser(), PARM_STARTAFTER, PARM_SKIP);
      _log.error(msg);
      requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).entity(msg).build());
      return;
    }
  }

  /**
   * Common checks for query parameters
   *   - Check that if parameter is present there is only one value
   * @param requestContext - context containing parameters
   * @param parmName - parameter to check
   * @return true if invalid, false if valid
   */
  private static boolean invalidParm(TapisThreadContext threadContext, ContainerRequestContext requestContext, String parmName)
  {
    MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters();
    // Check that it is a single value
    if (queryParameters.containsKey(parmName) && queryParameters.get(parmName).size() != 1)
    {
      String msg = MsgUtils.getMsg("TAPIS_QUERY_PARAM_MULTIPLE", threadContext.getJwtTenantId(), threadContext.getJwtUser(),
              threadContext.getOboTenantId(), threadContext.getOboUser(), parmName);
      _log.error(msg);
      requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).entity(msg).build());
      return true;
    }
    return false;
  }

  /**
   * Get query parameter if present
   * @param queryParameters - parameters from request context
   * @param parmName - parameter to retrieve
   * @return string value of parameter or null if parameter not present
   */
  private static String getQueryParm(MultivaluedMap<String, String> queryParameters, String parmName)
  {
    String parmValue = null;
    if (queryParameters.containsKey(parmName))
    {
      parmValue = queryParameters.get(parmName).get(0);
      _log.debug("Found query parameter. Name: " + parmName + " Value: " + parmValue);
    }
    return parmValue;
  }
}

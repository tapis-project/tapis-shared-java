package edu.utexas.tacc.tapis.sharedapi.security;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.sharedapi.security.TenantManager.RequestRoutingInfo;

/** This class ties together the TenantManager and ServiceJWT classes to provide all
 * the routing and authentication information needed to send a request to a service
 * running in a particular tenant.
 * 
 * The target tenant and service are specified on construction and used to obtain
 * the service's site and baseUrl.  If unable to obtain these values, the constructor
 * will throw an exception.
 * 
 * Instances of this class can be reused indefinitely as long as a site's services
 * aren't changed.  Each call to getAccessJWT() gets the latest JWT for the the
 * target service's site.
 * 
 * @author rcardone
 */
public class RequestRouter 
{
    /* **************************************************************************** */
    /*                                    Fields                                    */
    /* **************************************************************************** */
	// Input assignments.
	private final String      _tenant;
	private final String      _service;
	private final IServiceJWT _serviceJWT;
	
	// Derived from inputs.
	private final RequestRoutingInfo _routingInfo;
	
    /* **************************************************************************** */
    /*                                 Constructors                                 */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* constructor:                                                                 */
    /* ---------------------------------------------------------------------------- */
	public RequestRouter(String tenant, String service, IServiceJWT serviceJWT) 
	 throws RuntimeException, TapisException
	{
		// Assign inputs.
		_tenant     = tenant;
		_service    = service;
		_serviceJWT = serviceJWT;
		
		// Get the tenant manager's routing information.
		_routingInfo = TenantManager.getInstance().getRequestRoutingInfo(tenant, service);
	}

    /* **************************************************************************** */
    /*                                Public Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* getServiceBaseUrl:                                                           */
    /* ---------------------------------------------------------------------------- */
	public String getServiceBaseUrl() {return _routingInfo.getBaseUrl();}
	
    /* ---------------------------------------------------------------------------- */
    /* getServiceSite:                                                              */
    /* ---------------------------------------------------------------------------- */
	public String getServiceSite() {return _routingInfo.getTargetSiteId();}
	
    /* ---------------------------------------------------------------------------- */
    /* getAccessJWT:                                                                */
    /* ---------------------------------------------------------------------------- */
	public String getAccessJWT() {return _serviceJWT.getAccessJWT(getServiceSite());}
	
    /* **************************************************************************** */
    /*                                  Accessors                                   */
    /* **************************************************************************** */
	public String getTenant()  {return _tenant;}
	public String getService() {return _service;}
	public IServiceJWT getServiceJWT() {return _serviceJWT;}
}

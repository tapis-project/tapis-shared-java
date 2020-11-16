package edu.utexas.tacc.tapis.shared.security;

import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.runtime.TapisRuntimeException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;

public final class ServiceContext 
{
    /* ********************************************************************** */
	/*                               Constants                                */
	/* ********************************************************************** */
	// Tracing.
	private static final Logger _log = LoggerFactory.getLogger(ServiceContext.class);

    /* ********************************************************************** */
	/*                                Fields                                  */
	/* ********************************************************************** */
	// Set upon serviceJWT initialization.
	private String     _siteId;
	private ServiceJWT _serviceJWT;
	
	// Cache populated as JWTs for services in a tenant are requested.
	private final Hashtable<String,RequestRouter> _routerCache = new Hashtable<>();
	
	/* ********************************************************************** */
	/*                       SingletonInitializer class                       */
	/* ********************************************************************** */
	/** Bill Pugh method of singleton initialization. */
	private static final class SingletonInitializer
	{
		private static final ServiceContext _instance = new ServiceContext(); 
	}
	
	/* ********************************************************************** */
	/*                              Constructors                              */
	/* ********************************************************************** */
	/* ---------------------------------------------------------------------- */
	/* constructor:                                                           */
	/* ---------------------------------------------------------------------- */
	private ServiceContext(){}

	/* ********************************************************************** */
	/*                             Public Methods                             */
	/* ********************************************************************** */
	/* ---------------------------------------------------------------------- */
	/* getInstance:                                                           */
	/* ---------------------------------------------------------------------- */
	public static ServiceContext getInstance() {return SingletonInitializer._instance;}

	/* ---------------------------------------------------------------------- */
    /* initServiceJWT:                                                        */
	/* ---------------------------------------------------------------------- */
	/** Initialize the JWT manager responsible for acquiring and refreshing
	 * the service JWTs needed to communicate with other services at local and
	 * remote sites.
	 * 
	 * The password is not saved.  Initialization succeeds at most once. 
	 * 
	 * @param siteId the site at which the initializing service is running
	 * @param servicePassword the service password provided at start up
	 * @throws TapisRuntimeException
	 * @throws TapisException
	 * @throws TapisClientException
	 */
	public synchronized void initServiceJWT(String siteId, String servicePassword) 
	 throws TapisRuntimeException, TapisException, TapisClientException
	{
		// Check input.
		if (StringUtils.isBlank(siteId)) {
		    String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "initServiceJWT", "siteId");
		    throw new TapisException(msg);
		}
		if (StringUtils.isBlank(servicePassword)) {
		    String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "initServiceJWT", "servicePassword");
		    throw new TapisException(msg);
		}
		
		// One-time initialization enforced.
		if (_serviceJWT != null) return;
		
		// Get the master tenant id of the site at which we are running. 
		var tenantId = TenantManager.getInstance().getSiteMasterTenantId(siteId);
		if (StringUtils.isBlank(tenantId)) {
		     String msg = MsgUtils.getMsg("TAPIS_SITE_NO_MASTER_TENANT",
		    		                      siteId, tenantId);
		     throw new TapisException(msg);
		}
		   
		// Get the site master tenant object.
		var tenant =  TenantManager.getInstance().getTenant(tenantId);
		if (tenant == null) {
		     String msg = MsgUtils.getMsg("TAPIS_TENANT_NOT_FOUND", tenantId);
		     throw new TapisException(msg);
		}
		   
		// Get the list of all sites.
		var sites = new ArrayList<String>(TenantManager.getInstance().getSites().keySet());
		  
		// Assemble the parameters for the service JWT manager.
		var jwtParms = new ServiceJWTParms();
		jwtParms.setTenant(tenantId);
		jwtParms.setTargetSites(sites);
		jwtParms.setTokensBaseUrl(tenant.getBaseUrl());
		jwtParms.setServiceName(TapisConstants.SERVICE_NAME_JOBS);
		  
		// Create the manager and complete initialization.
		_serviceJWT = new ServiceJWT(jwtParms, servicePassword);
		_siteId = siteId;
	}

	/* ---------------------------------------------------------------------- */
    /* getRouter:                                                             */
	/* ---------------------------------------------------------------------- */
	/** Get the router that calculates the JWT needed to communicate with a
	 * given service and tenant.
	 * 
	 * @param tenant the tenant of the target service
	 * @param service the target service
	 * @return a router object
	 * @throws RuntimeException
	 * @throws TapisException
	 */
	public RequestRouter getRouter(String tenant, String service) 
     throws RuntimeException, TapisException
	{
		// Sanity check.
		if (_serviceJWT == null) 
			throw new TapisRuntimeException(
			    MsgUtils.getMsg("TAPIS_OBJECT_NOT_INITIALIZED", 
			    		        this.getClass().getSimpleName()));
		
		// See if have already calculated this route.
		var routerKey = makeRouterKey(tenant, service);
		var router = _routerCache.get(routerKey);
		
		// Create and cache a new router if necessary.
		if (router == null) {
			router = new RequestRouter(tenant, service, _serviceJWT);
			_routerCache.put(routerKey, router);
		}
		
		return router;
	}
	
	/* ---------------------------------------------------------------------- */
    /* Accessors:                                                             */
	/* ---------------------------------------------------------------------- */
	public String getSiteId() {return _siteId;}
	public ServiceJWT getServiceJWT() {return _serviceJWT;}
	
	/* ********************************************************************** */
	/*                             Private Methods                            */
	/* ********************************************************************** */
	/* ---------------------------------------------------------------------- */
    /* makeRouterKey:                                                         */
	/* ---------------------------------------------------------------------- */
	private String makeRouterKey(String tenant, String service)
	{
		return service + "@" + tenant;
	}
}

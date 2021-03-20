package edu.utexas.tacc.tapis.shared.security.refresh;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.runtime.TapisRuntimeException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.security.RequestRouter;
import edu.utexas.tacc.tapis.shared.security.ServiceJWT;
import edu.utexas.tacc.tapis.shared.security.ServiceJWTParms;
import edu.utexas.tacc.tapis.shared.security.TenantManager;

/** This class is a copy of the ServiceContext altered for testing the token 
 * refresh process.  The approach is to tweak the original class with changes
 * that allow tokens to be refresh on a short time scale so as to be easily
 * observable.  
 * 
 * Alterations to the original code that affect execution are annotated 
 * with ^^^^^.  These changes can be tuned for effect for testing.
 * 
 * See TokenRefreshTest for details.
 * 
 * @author rcardone
 */
public class MockServiceContext 
{
    /* ********************************************************************** */
	/*                               Constants                                */
	/* ********************************************************************** */
	// Tracing.
	private static final Logger _log = LoggerFactory.getLogger(MockServiceContext.class);

    // ^^^^^ Test TTL for tokens.
    private static final int ACCESS_TOKEN_TTL_SECS = 30;
    
    /* ********************************************************************** */
	/*                                Fields                                  */
	/* ********************************************************************** */
	// Set upon serviceJWT initialization.
	private String     _siteId;
	private ServiceJWT _serviceJWT;
	private Instant    _lastServiceJWTRefresh;  // synchronized access
	
	// Cache populated as JWTs for services in a tenant are requested.
	private final Hashtable<String,RequestRouter> _routerCache = new Hashtable<>();
	
	/* ********************************************************************** */
	/*                       SingletonInitializer class                       */
	/* ********************************************************************** */
	/** Bill Pugh method of singleton initialization. */
	private static final class SingletonInitializer
	{
		private static final MockServiceContext _instance = new MockServiceContext(); 
	}
	
	/* ********************************************************************** */
	/*                              Constructors                              */
	/* ********************************************************************** */
	/* ---------------------------------------------------------------------- */
	/* constructor:                                                           */
	/* ---------------------------------------------------------------------- */
	private MockServiceContext(){}

	/* ********************************************************************** */
	/*                             Public Methods                             */
	/* ********************************************************************** */
	/* ---------------------------------------------------------------------- */
	/* getInstance:                                                           */
	/* ---------------------------------------------------------------------- */
	public static MockServiceContext getInstance() {return SingletonInitializer._instance;}

	/* ---------------------------------------------------------------------- */
    /* getAccessJWT:                                                          */
	/* ---------------------------------------------------------------------- */
	/** Get the calling service's serialized JWT for a given service and tenant.
	 * 
	 * @param tenant the tenant of the target service
	 * @param service the target service 
	 * @return the serialized access JWT 
	 * @throws TapisException 
	 * @throws RuntimeException 
	 */
	public String getAccessJWT(String tenant, String service) 
     throws RuntimeException, TapisException
	{
		return getRouter(tenant, service).getAccessJWT();
	}
	
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
	 * @param service the name of the call service whose JWTs will be created
	 * @param servicePassword the service password provided at start up
	 * @throws TapisRuntimeException
	 * @throws TapisException
	 * @throws TapisClientException
	 */
	public synchronized void initServiceJWT(String siteId, String service, 
			                                String servicePassword) 
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
		
		// Get the admin tenant id of the site at which we are running. 
		var tenantId = TenantManager.getInstance().getSiteAdminTenantId(siteId);
		if (StringUtils.isBlank(tenantId)) {
		     String msg = MsgUtils.getMsg("TAPIS_SITE_NO_ADMIN_TENANT",
		    		                      siteId, tenantId);
		     throw new TapisException(msg);
		}
		   
		// Get the site admin tenant object.
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
		jwtParms.setServiceName(service);
		
        // ^^^^^ TEST SETTINGS
        jwtParms.setAccessTTL(ACCESS_TOKEN_TTL_SECS);   // ttl in seconds
        jwtParms.setRefreshTTL(ACCESS_TOKEN_TTL_SECS + 5);  // ttl in seconds
        
		// Create the manager and complete initialization.
		_serviceJWT = new ServiceJWT(jwtParms, servicePassword);
		_lastServiceJWTRefresh = _serviceJWT.getLastRefreshTime();
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
    /* hasRefreshedTokens:                                                    */
    /* ---------------------------------------------------------------------- */
	/** Determine if the serviceJWT has refreshed its tokens since the last 
	 * time this method was called.  If so, the new token refresh time is 
	 * recorded and true is returned indicating to the call that any tokens it
	 * has cache should be discarded.  If the last refresh time has not changed,
	 * then false is returned and the caller can continue using any previously
	 * acquired tokens.
	 * 
	 * This method is threadsafe so that only one thead at a time determines 
	 * if the refresh time has changed.
	 * 
	 * @return true if the tokens have been refreshed, false otherwise
	 */
	public synchronized boolean hasRefreshedTokens()
	{
	    // See if a token refresh operation has occurred since we last checked.
	    var curRefreshTime = _serviceJWT.getLastRefreshTime();
	    if (_lastServiceJWTRefresh.equals(curRefreshTime)) return false;
	    
	    // Update the last update time and indicate to the caller that tokens 
	    // have been refreshed and the new last update time has been recorded.
	    _lastServiceJWTRefresh = curRefreshTime;
	    return true;
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

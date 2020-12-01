package edu.utexas.tacc.tapis.shared.security;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;

import edu.utexas.tacc.tapis.apps.client.AppsClient;
import edu.utexas.tacc.tapis.auth.client.AuthClient;
import edu.utexas.tacc.tapis.files.client.FilesClient;
import edu.utexas.tacc.tapis.meta.client.MetaClient;
import edu.utexas.tacc.tapis.security.client.SKClient;
import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.systems.client.SystemsClient;
import edu.utexas.tacc.tapis.tenants.client.TenantsClient;
import edu.utexas.tacc.tapis.tokens.client.TokensClient;

/** This class manages client instance for all the Tapis services.  Each client
 * object is initialized with the proper JWT for the tenant-specific site on 
 * which the service is configured.  In addition, each client is configured for
 * a specific user and tenant via OBO settings.
 * 
 * Cache replacement employs a least recently used (LRU) policy when the number
 * of clients reaches the maximum allowed.  In addition, each client has a TTL
 * that when expired will also cause it to be removed from the cache. The maximum 
 * cache size and TTL values are currently hardcoded.
 * 
 * This class requires the singleton instance of the ServiceContext class to have
 * been initialized prior to calls to getClient().  Note that ServiceContext 
 * initialization includes creating a ServiceJWT instance.
 * 
 * @author rcardone
 */
public class ServiceClients 
{
    /* ********************************************************************** */
	/*                               Constants                                */
	/* ********************************************************************** */
	// Tracing.
	private static final Logger _log = LoggerFactory.getLogger(ServiceClients.class);
	
	// Separator character used in keys.
	private static final String SEP = "|";
	
	// Cache constraints.
	private static final int MAX_CLIENTS = 150;
	private static final int MAX_MINUTES = 10;

    /* ********************************************************************** */
	/*                                Fields                                  */
	/* ********************************************************************** */
	// The regex pattern used to split a cache key into its constituent parts.
	private final Pattern _keyPattern = Pattern.compile("\\" + SEP);
	
	// Mapping of service client classes to service names.  The map needs to be
	// manually updated when a new client is added.
	private final HashMap<Class<?>,String> _class2ServiceMap = initClass2ServiceMap();
	
	// This is a cache of client objects that are specific to each user/tenant
	// combination.  Clients are site specific via their base URL setting.
	// The key is a concatenation of user, tenant and service.  The value is
	// the client object for a service customized for a particular user@tenant.
	private final LoadingCache<String,Object> _clientCache = initCache();

	/* ********************************************************************** */
	/*                       SingletonInitializer class                       */
	/* ********************************************************************** */
	/** Bill Pugh method of singleton initialization. */
	private static final class SingletonInitializer
	{
		private static final ServiceClients _instance = new ServiceClients(); 
	}
	
	/* ********************************************************************** */
	/*                              Constructors                              */
	/* ********************************************************************** */
	/* ---------------------------------------------------------------------- */
	/* constructor:                                                           */
	/* ---------------------------------------------------------------------- */
	private ServiceClients(){}

	/* ********************************************************************** */
	/*                             Public Methods                             */
	/* ********************************************************************** */
	/* ---------------------------------------------------------------------- */
	/* getInstance:                                                           */
	/* ---------------------------------------------------------------------- */
	public static ServiceClients getInstance() {return SingletonInitializer._instance;}

	/* ********************************************************************** */
	/*                             Public Methods                             */
	/* ********************************************************************** */
	/* ---------------------------------------------------------------------- */
	/* getClient:                                                             */
	/* ---------------------------------------------------------------------- */
	/** Return a typed service client instance.  The client class input parameter
	 * determines the return type.  Only client classes that have been mapped
	 * to service names will be recognized.
	 * 
	 * @param <T> the service's client class type 
	 * @param tenant the tenant of the service
	 * @param user the user on behalf of whom the service is being called
	 * @param cls the service's client class type
	 * @return
	 * @throws RuntimeException
	 * @throws TapisException
	 * @throws ExecutionException 
	 */
	@SuppressWarnings("unchecked")
	public <T> T getClient(String user, String tenant, Class<T> cls) 
	 throws RuntimeException, TapisException, ExecutionException
	{
		// Map the client class to its service name.
		String serviceName = _class2ServiceMap.get(cls);
		if (serviceName == null)
			throw new TapisException(MsgUtils.getMsg("TAPIS_UNKNOWN_CLIENT_CLASS", 
					                                 cls.getSimpleName()));
		
		// Return the typed client.
		return (T) getClient(user, tenant, serviceName);
	}
	
	/* ---------------------------------------------------------------------- */
	/* getClient:                                                             */
	/* ---------------------------------------------------------------------- */
	/** Return an untyped service client instance configured for the specified
	 * tenant and user.  If the service is known, it determines the return type
	 * of the client.
	 * 
	 * @param user the user on behalf of whom the service is being called
	 * @param tenant the tenant of the service
	 * @param service the name of the target service
	 * @return the target service's client
	 * @throws RuntimeException
	 * @throws TapisException
	 * @throws ExecutionException 
	 */
	public Object getClient(String user, String tenant, String service) 
     throws RuntimeException, TapisException, ExecutionException
	{
	    // Guard against bogus input.
	    if (StringUtils.isBlank(user))
            throw new TapisException(MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "getClient", "user"));
        if (StringUtils.isBlank(tenant))
            throw new TapisException(MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "getClient", "tenant"));
        if (StringUtils.isBlank(service))
            throw new TapisException(MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "getClient", "service"));
	    
		// See if we already have the client.
		String key = getCacheKey(user, tenant, service);
		return _clientCache.get(key);
	}
	
	/* ---------------------------------------------------------------------- */
	/* removeClient:                                                          */
	/* ---------------------------------------------------------------------- */
	/** Remove the client from the cache if it exists and return it.  Return
	 * null if it doesn't exist. 
	 * 
	 * @param user the user on behalf of whom the service is being called
	 * @param tenant the client's tenant
	 * @param service the client's target service name
	 */
	public void removeClient(String user, String tenant, String service)
	{
		// Attempt to remove the client.  No attempt is made
		// to free any resources controlled by the client.
		String key = getCacheKey(user, tenant, service);
		_clientCache.invalidate(key);
	}
	
    /* ---------------------------------------------------------------------- */
    /* getStats:                                                              */
    /* ---------------------------------------------------------------------- */
	/** Get cache usage information.
	 * @return the guava cache statistics
	 */
	public CacheStats getStats() {return _clientCache.stats();}
	
	/* ********************************************************************** */
	/*                            Private Methods                             */
	/* ********************************************************************** */
	/* ---------------------------------------------------------------------- */
	/* getCacheKey:                                                           */
	/* ---------------------------------------------------------------------- */
	private String getCacheKey(String user, String tenant, String service)
	{return user + SEP + tenant + SEP + service;}
	
    /* ---------------------------------------------------------------------- */
    /* initCache:                                                             */
    /* ---------------------------------------------------------------------- */
	/** Create the client object cache with a limit on the maximum number of
	 * clients cached and a TTL on each client.
	 * 
	 * @return the cache
	 */
	private LoadingCache<String,Object> initCache()
	{
	    // Create the cache of string keys to service client objects.
	    LoadingCache<String,Object> cache = CacheBuilder.newBuilder()
	        .maximumSize(MAX_CLIENTS)
	        .expireAfterAccess(MAX_MINUTES, TimeUnit.MINUTES)
	        .recordStats()
	        .build(new CacheLoader<String, Object>() {
	             @Override
	             public Object load(String key) throws Exception {
	               return loadClient(key);
	             }
	           });
	    
	    return cache;
	}
	
    /* ---------------------------------------------------------------------- */
    /* loadClient:                                                            */
    /* ---------------------------------------------------------------------- */
    /** Return a new untyped service client instance configured for the specified
     * tenant and user.  This method is called by the loading cache on cache 
     * misses.
     * 
     * @param key the cache key for the new client
     * @return the target service's client
     * @throws RuntimeException
     * @throws TapisException
     */
    private Object loadClient(String key) 
     throws RuntimeException, TapisException
    {
        // Split the key into its 3 parts.
        String[] parts = _keyPattern.split(key);
        String user    = parts[0];
        String tenant  = parts[1];
        String service = parts[2];
        
        // Create a new client for this service/tenant combination.
        var router = ServiceContext.getInstance().getRouter(tenant, service);
        
        // Get the client.
        Object client = null;
        switch (service)
        {
            case TapisConstants.SERVICE_NAME_APPS: {
                var clt = new AppsClient(router.getServiceBaseUrl(), router.getAccessJWT());
                clt.addDefaultHeader("X-Tapis-User", user);
                clt.addDefaultHeader("X-Tapis-Tenant", tenant);
                client = clt;
                break;
            }
                
            case TapisConstants.SERVICE_NAME_JOBS: {
//              var clt = new JobsClient(router.getServiceBaseUrl(), router.getAccessJWT());
                break;
            }
                
            case TapisConstants.SERVICE_NAME_SECURITY: {
                var clt = new SKClient(router.getServiceBaseUrl(), router.getAccessJWT());
                clt.addDefaultHeader("X-Tapis-User", user);
                clt.addDefaultHeader("X-Tapis-Tenant", tenant);
                client = clt;
                break;
            }
                
            case TapisConstants.SERVICE_NAME_SYSTEMS: {
                var clt = new SystemsClient(router.getServiceBaseUrl(), router.getAccessJWT());
                clt.addDefaultHeader("X-Tapis-User", user);
                clt.addDefaultHeader("X-Tapis-Tenant", tenant);
                client = clt;
                break;
            }
            
            case TapisConstants.SERVICE_NAME_AUTHN: {
                var clt = new AuthClient(router.getServiceBaseUrl());
                clt.addDefaultHeader("X-Tapis-User", user);
                clt.addDefaultHeader("X-Tapis-Tenant", tenant);
                client = clt;
                break;
            }
                
            case TapisConstants.SERVICE_NAME_TENANTS: {
                var clt = new TenantsClient(router.getServiceBaseUrl());
                clt.addDefaultHeader("X-Tapis-User", user);
                clt.addDefaultHeader("X-Tapis-Tenant", tenant);
                client = clt;
                break;
            }
                
            case TapisConstants.SERVICE_NAME_TOKENS: {
                var clt = new TokensClient(router.getServiceBaseUrl());
                clt.addDefaultHeader("X-Tapis-User", user);
                clt.addDefaultHeader("X-Tapis-Tenant", tenant);
                client = clt;
                break;
            }
                
            case TapisConstants.SERVICE_NAME_META: {
                var clt = new MetaClient(router.getServiceBaseUrl(), router.getAccessJWT());
                clt.addDefaultHeader("X-Tapis-User", user);
                clt.addDefaultHeader("X-Tapis-Tenant", tenant);
                client = clt;
                break;
            }   
                
            case TapisConstants.SERVICE_NAME_FILES:
                var clt = new FilesClient(router.getServiceBaseUrl(), router.getAccessJWT());
                clt.addDefaultHeader("X-Tapis-User", user);
                clt.addDefaultHeader("X-Tapis-Tenant", tenant);
                client = clt;
                break;
                
            default:    
        }
        
        // Make sure we found the client.
        if (client == null) 
            throw new TapisException(MsgUtils.getMsg("TAPIS_CLIENT_NOT_FOUND", service, tenant, user));
        
        // Return the client.
        return client;
    }
    
	/* ---------------------------------------------------------------------- */
	/* initClass2ServiceMap :                                                 */
	/* ---------------------------------------------------------------------- */
	/** Return the mapping of service client classes to service names.
	 * 
	 * Make sure to add new clients to this map as they become available.
	 * 
	 * @return the service associated with this client.
	 */
	private HashMap<Class<?>,String> initClass2ServiceMap()
	{
		var map = new HashMap<Class<?>,String>(19);
		map.put(AppsClient.class, TapisConstants.SERVICE_NAME_APPS);
		map.put(SKClient.class, TapisConstants.SERVICE_NAME_SECURITY);
		map.put(SystemsClient.class, TapisConstants.SERVICE_NAME_SYSTEMS);
		map.put(AuthClient.class, TapisConstants.SERVICE_NAME_AUTHN);
		map.put(TenantsClient.class, TapisConstants.SERVICE_NAME_TENANTS);
		map.put(TokensClient.class, TapisConstants.SERVICE_NAME_TOKENS);
		map.put(MetaClient.class, TapisConstants.SERVICE_NAME_META);
		map.put(FilesClient.class, TapisConstants.SERVICE_NAME_FILES);
		
		return map;
	}
}

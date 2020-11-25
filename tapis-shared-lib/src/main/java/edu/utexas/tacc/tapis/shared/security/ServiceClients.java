package edu.utexas.tacc.tapis.shared.security;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class ServiceClients 
{
    /* ********************************************************************** */
	/*                               Constants                                */
	/* ********************************************************************** */
	// Tracing.
	private static final Logger _log = LoggerFactory.getLogger(ServiceClients.class);

    /* ********************************************************************** */
	/*                                Fields                                  */
	/* ********************************************************************** */
	// Mapping of service client classes to service names.
	private HashMap<Class<?>,String> _class2ServiceMap = initClass2ServiceMap();
	
	// This is a cache of client objects that are specific to each user/tenant
	// combination.  Clients are site specific via their base URL setting.
	// The key is a concatenation of user, tenant and service.  The value is
	// the client object for a service customized for a particular user@tenant.
	private HashMap<String,Object> _clientCache = new HashMap<String,Object>();

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
	 */
	@SuppressWarnings("unchecked")
	public <T> T getClient(String tenant, String user, Class<T> cls) 
	 throws RuntimeException, TapisException
	{
		// Map the client class to its service name.
		String serviceName = _class2ServiceMap.get(cls);
		if (serviceName == null)
			throw new TapisException(MsgUtils.getMsg("TAPIS_UNKNOWN_CLIENT_CLASS", 
					                                 cls.getSimpleName()));
		
		// Return the typed client.
		return (T) getClient(tenant, user, serviceName);
	}
	
	/* ---------------------------------------------------------------------- */
	/* getClient:                                                             */
	/* ---------------------------------------------------------------------- */
	/** Return an untyped service client instance configured for the specified
	 * tenant and user.  If the service is known, it determines the return type
	 * of the client.
	 * 
	 * @param tenant the tenant of the service
	 * @param user the user on behalf of whom the service is being called
	 * @param service the name of the target service
	 * @return the target service's client
	 * @throws RuntimeException
	 * @throws TapisException
	 */
	public Object getClient(String tenant, String user, String service) 
     throws RuntimeException, TapisException
	{
		// See if we already have the client.
		String key = getCacheKey(tenant, user, service);
		Object client = _clientCache.get(key);
		if (client != null) return client;
		
		// Create a new client for this service/tenant combination.
		var router = ServiceContext.getInstance().getRouter(tenant, service);
		
		// Get the client.
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
//				var clt = new JobsClient(router.getServiceBaseUrl(), router.getAccessJWT());
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
			throw new TapisException(MsgUtils.getMsg("TAPIS_CLIENT_NOT_FOUND", service, tenant));
		
		// Cache the client.
		_clientCache.put(key, client);
		return client;
	}
	
	/* ---------------------------------------------------------------------- */
	/* removeClient:                                                          */
	/* ---------------------------------------------------------------------- */
	/** Remove the client from the cache if it exists and return it.  Return
	 * null if it doesn't exist. 
	 * 
	 * @param tenant the client's tenant
	 * @param service the client's target service
	 */
	public Object removeClient(String tenant, String user, String service)
	{
		// Attempt to remove the client.  No attempt is made
		// to free any resources controlled by the client.
		String key = getCacheKey(tenant, user, service);
		return _clientCache.remove(key);
	}
	
	/* ********************************************************************** */
	/*                            Private Methods                             */
	/* ********************************************************************** */
	/* ---------------------------------------------------------------------- */
	/* getCacheKey:                                                           */
	/* ---------------------------------------------------------------------- */
	private String getCacheKey(String tenant, String user, String service)
	{return user + "@" + tenant + ":" + service;}
	
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
		
		return map;
	}
}

package edu.utexas.tacc.tapis.shared.security;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.utexas.tacc.tapis.shared.TapisConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.runtime.TapisRuntimeException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.utils.CallSiteToggle;
import edu.utexas.tacc.tapis.tenants.client.TenantsClient;
import edu.utexas.tacc.tapis.tenants.client.gen.model.Site;
import edu.utexas.tacc.tapis.tenants.client.gen.model.Tenant;


public class TenantManager
 implements ITenantManager 
{
    /* **************************************************************************** */
    /*                                   Constants                                  */
    /* **************************************************************************** */
    // Local logger.
    private static final Logger _log = LoggerFactory.getLogger(TenantManager.class);
    
    // Tenants service path.
    private static final String TENANTS_PATH = "v3/tenants";
    
    // Minimum time allowed between refreshes.
    private static final long MIN_REFRESH_SECONDS = 600; // 10 minutes

	// The text to be replaced in the url templates defined in site objects.
	private static final String BASEURL_PLACEHOLDER = "${tenant_id}";
	
    /* **************************************************************************** */
    /*                                    Fields                                    */
    /* **************************************************************************** */
    // Singleton instance.
    private static TenantManager     _instance;
    
    // Base url for the tenant's service.
    private final String             _tenantServiceBaseUrl;
    
    // The map tenant ids to tenants retrieved from the tenant's service.
    private Map<String,Tenant>       _tenants;
    
    // The map site ids to sites retrieved from the tenant's service.
    private Map<String,Site>         _sites;
    
    // The primary site for this Tapis instance.
    private String                   _primarySiteId;
    private Site                     _primarySite;
    
    // The map of site admin tenant keys to list of tenant values that the site
    // admin tenant is allowed to act on behalf of.  
    private Map<String,List<String>> _allowableTenants;
    
    // Time of the last update.
    private Instant                  _lastUpdateTime;
    
    // Toggle switch that limits log output.
    private static final CallSiteToggle _lastGetTenantsSucceeded = new CallSiteToggle();
    
    /* **************************************************************************** */
    /*                                 Constructors                                 */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* constructor:                                                                 */
    /* ---------------------------------------------------------------------------- */
    private TenantManager(String tenantServiceBaseUrl)
    {
        // Make sure the url ends with a slash.
        if (!tenantServiceBaseUrl.endsWith("/")) tenantServiceBaseUrl += "/";
        _tenantServiceBaseUrl = tenantServiceBaseUrl;
    }
    
    /* **************************************************************************** */
    /*                                Public Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* getInstance:                                                                 */
    /* ---------------------------------------------------------------------------- */
    public static TenantManager getInstance(String tenantServiceBaseUrl)
     throws TapisRuntimeException
    {
        // Only create a new instance if one doesn't exist.
        if (_instance == null) {
            
            // The base url to the tenants service must be provided.
            if (StringUtils.isBlank(tenantServiceBaseUrl)) {
                String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "getInstance", 
                                             "tenantServiceBaseUrl");
                _log.error(msg);
                throw new TapisRuntimeException(msg);
            }
            
            // Enter the monitor and check instance again before taking action.
            synchronized (TenantManager.class) {
                if (_instance == null)
                    _instance = new TenantManager(tenantServiceBaseUrl);
            }
        }
        return _instance;
    }

    /* ---------------------------------------------------------------------------- */
    /* getInstance:                                                                 */
    /* ---------------------------------------------------------------------------- */
    /** Only use this if you KNOW the singleton instance has been initialized.
     * 
     * @return the non-null instance
     * @throws TapisRuntimeException if the tenants manager instance does not exist
     */
    public static TenantManager getInstance() throws TapisRuntimeException
    {
        // Throw runtime exception if we are not initialized.
        if (_instance == null) {
            String msg = MsgUtils.getMsg("TAPIS_TENANT_MGR_NOT_INITIALIZED");
            _log.error(msg);
            throw new TapisRuntimeException(msg);
        }
        return _instance;
    }
    
    /* ---------------------------------------------------------------------------- */
    /* getTenants:                                                                  */
    /* ---------------------------------------------------------------------------- */
    /** Return the map of tenant ids to tenant objects of all known tenants. This 
     * method is typically used by services to force the initialization of the tenants 
     * map.  If the map hasn't been retrieved from the tenants service, it will be 
     * downloaded.  Otherwise, the previously downloaded map will be returned. 
     * 
     * @return the tenants map
     * @throws TapisRuntimeException if the list cannot be attained
     */
    @Override
    public Map<String,Tenant> getTenants() throws TapisRuntimeException
    {
        // Initialize the list if necessary.
        if (_tenants == null) {
            synchronized (TenantManager.class) {
                // Avoid race condition.
                if (_tenants == null) {
                    try {
                        // Get the tenant and site lists from the tenant service.
                        var tenantsClient = new TenantsClient(_tenantServiceBaseUrl);
                        var tenantList = tenantsClient.getTenants();
                        var siteList   = tenantsClient.getSites();
                        
                        // Create the tenants hashmap.
                        _tenants = new LinkedHashMap<String,Tenant>(1+tenantList.size()*2);
                        for (Tenant t : tenantList) _tenants.put(t.getTenantId(), t); 
                        
                        // Create the sites hashmap.
                        _sites = new LinkedHashMap<String,Site>(1+siteList.size()*2);
                        for (Site s : siteList) _sites.put(s.getSiteId(), s); 
                        
                        // Check the tenant's information for consistency.
                        checkTenantMaps(_tenants, _sites);
                        
                        // Calculate allowable tenants map and set primary site.
                        _allowableTenants = calculateAllowableTenants(_tenants, _sites);
                                                
                    } catch (Exception e) {
                        String msg = MsgUtils.getMsg("TAPIS_TENANT_LIST_ERROR",
                                                     getTenantsPath());
                        if (_lastGetTenantsSucceeded.toggleOff()) _log.error(msg, e);
                        throw new TapisRuntimeException(msg, e);
                    }
                    
                    // Mark the time of the download.
                    _lastUpdateTime = Instant.now();
                    _lastGetTenantsSucceeded.toggleOn();
                    
                    // Write a message to the log.
                    if (_log.isInfoEnabled())
                        _log.info(MsgUtils.getMsg("TAPIS_TENANT_LIST_RECIEVED",
                                                  getTenantsPath()));
                }
            }
        }
        
        return _tenants;
    }
    
    /* ---------------------------------------------------------------------------- */
    /* refreshTenants:                                                              */
    /* ---------------------------------------------------------------------------- */
    /** This method forces a refresh of the tenants map as long as the minimum 
     * update interval has been exceeded. The map is from tenant ids to tenant objects.
     * Clients typically don't need to call this method as it will be automatically 
     * called if a tenant is not found.
     * 
     * When an event bus is integrated into Tapis, this method can be replaced by
     * event triggered refreshes.
     * 
     * @return a tenants list that may have been refreshed
     * @throws TapisRuntimeException if a map cannot be attained
     */
    @Override
    public Map<String,Tenant> refreshTenants() throws TapisRuntimeException
    {
    	// Synchronize on this class object just like in getTenants().
    	synchronized (TenantManager.class) {
    		// Maybe we are not initialized.
    		if (_tenants == null) return getTenants();
        
    		// Guard against denial of service attacks.
    		if (!allowRefresh()) return getTenants();
        
    		// Clear and repopulate the stale list.
            clear();
            return getTenants();
        }
    }
    
    /* ---------------------------------------------------------------------------- */
    /* getTenant:                                                                   */
    /* ---------------------------------------------------------------------------- */
    /** Get a tenant definition from the cached list.  If the tenant is not found in
     * the list, or if the list has not been initialized, an attempt to retrieve the
     * current list will be made if the minimum refresh interval has expired.
     * 
     * @param tenantId the id of the tenant 
     * @return the non-null tenant
     * @throws TapisException if no tenant can be returned
     */
    @Override
    public Tenant getTenant(String tenantId) throws TapisException
    {
        // See if we can find the tenant.
        var tenants = getTenants();
        Tenant t = tenants.get(tenantId);
        if (t != null) return t;
        
        // The tenant was not found, maybe a refresh will help.
        tenants = refreshTenants();
        t = tenants.get(tenantId);
        if (t != null) return t;
        
        // Throw an exception if we can't find the tenant.
        String msg = MsgUtils.getMsg("TAPIS_TENANT_LIST_ERROR",
                                     getTenantsPath());
        _log.error(msg);
        throw new TapisException(msg);
    }
    
    /* ---------------------------------------------------------------------------- */
    /* allowTenantId:                                                               */
    /* ---------------------------------------------------------------------------- */
    /** Is the tenant specified in the JWT, jwtTenantId, allowed to specify the 
     * newTenantId in the X-Tapis-Tenant header or as a delegated tenant?  This method 
     * calculates whether a service or user in one tenant can make a request on behalf 
     * of a servie or user in another tenant. 
     * 
     * If the number of allowable tenants become large (in the 100s), it may be
     * desirable to use hashes rather than lists to improve look up time. 
     * 
     * @param jwtTenantId the tenant contained in a JWT's tapis/tenant_id claim
     * @param newTenantId the tenant on behalf of whom a request is being made
     * @return true if the tenant substitution is allowed, false otherwise
     * @throws TapisException if the jwt tenant object cannot be retrieved 
     */
    @Override
    public boolean allowTenantId(String jwtTenantId, String newTenantId)
     throws TapisException
    {
    	// Easy case.
    	if (jwtTenantId.equals(newTenantId)) return true;
    	
    	// Use the precalculated mapping of site admin tenants to their
    	// allowable tenants.
    	var allowableTenantList = _allowableTenants.get(jwtTenantId);
    	if (allowableTenantList != null && allowableTenantList.contains(newTenantId))
    		return true;

    	// The jwt tenant cannot act on behalf of the specified new tenant.
        return false;
    }
    
    /* ---------------------------------------------------------------------------- */
    /* getRequestRoutingInfo:                                                       */
    /* ---------------------------------------------------------------------------- */
    /** Determine the site that a request to the specified tenant/service combination 
     * should be sent.  Calculate the base url for the service on that site.  Return
     * the input parameters, the baseUrl and the targetSiteId.  This last value can
     * be used to select the proper JWT from a ServiceJWT instance.
     * 
     * Minimal checking is performed, exceptions are passed up.
     * 
     * Routing Background<br> 
     * ------------------<br> 
     * The correct routing of requests between associate sites and the primary site
     * assumes the following:
     * 
     *   - The primary site runs all services (or least all services that will 
     *     ever be accessed by a tapis service running at another site).
     *     
     *   - The primary site registers DNS names that conform to its base URL
     *     template, as many as one for each tenant.  These DNS names point to a
     *     proxy that forwards requests to the proper services.
     *     
     * Given the above, requests are normally routed to the tenant's base URL, which 
     * are then forwarded to specific services.  The one exception is when a service
     * for a tenant owned by an associate site does not actually run at the 
     * associate site, but instead runs at the primary site.  In this case, the request 
     * must target a URL at the primary site.  The URL targeted is the previously 
     * registered DNS address that conforms to the primary site's URL template for 
     * the request's tenant.  
     * 
     * @param tenantId the non-null request tenant, typically the oboTenant
     * @param service the non-null requested service
     * @return a cacheable result object
     * @throws TapisException on error.
     */
    public RequestRoutingInfo getRequestRoutingInfo(String tenantId, String service)
      throws TapisException
    {
    	// Determine the tenant and the site.
    	var tenant = getTenant(tenantId);
    	Site owningSite = getSite(tenant.getSiteId());
    	Site targetSite;
    	if (!owningSite.getServices().contains(service)) targetSite = getPrimarySite();
    	  else targetSite = owningSite; 
    	
		// If the tenant's owning site is different from the target site, then the target
    	// site must be the primary site because of the previous conditional (and the 
    	// invariant that the primary site runs all services). In this case, we construct 
    	// the base url from the target site's base url template.  Otherwise, the target 
    	// site is the site that owns the tenant, so we can directly use the tenant 
    	// assigned base url.
    	String baseUrl;
		if (targetSite != owningSite) 
			baseUrl = targetSite.getTenantBaseUrlTemplate().replace(BASEURL_PLACEHOLDER, tenantId);
		else 
			baseUrl = tenant.getBaseUrl();

        // TODO: remove special handling for SK, Jobs and Meta
        // TODO:
        // TODO: For some services we need to add /v3
        // TODO: Fixing this in each service will be a major effort
        // TODO:   so for now add the /v3 as needed.
        if (TapisConstants.SERVICE_NAME_SECURITY.equals(service) ||
            TapisConstants.SERVICE_NAME_JOBS.equals(service) ||
            TapisConstants.SERVICE_NAME_META.equals(service))
        {
          baseUrl = baseUrl + "/v3";
        }

		// Package up the results.
		return new RequestRoutingInfo(tenantId, service, baseUrl, targetSite.getSiteId());
    }
    
    /* ---------------------------------------------------------------------------- */
    /* getSites:                                                                    */
    /* ---------------------------------------------------------------------------- */
    @Override
    public Map<String,Site> getSites(){return _sites;}
    
    /* ---------------------------------------------------------------------------- */
    /* getSite:                                                                    */
    /* ---------------------------------------------------------------------------- */
    @Override
    public Site getSite(String siteId){return _sites.get(siteId);}
        
    /* ---------------------------------------------------------------------------- */
    /* getTenantServiceBaseUrl:                                                     */
    /* ---------------------------------------------------------------------------- */
    @Override
    public String getTenantServiceBaseUrl() {return _tenantServiceBaseUrl;}

    /* ---------------------------------------------------------------------------- */
    /* getLastUpdateTime:                                                           */
    /* ---------------------------------------------------------------------------- */
    @Override
    public Instant getLastUpdateTime() {return _lastUpdateTime;}

    /* ---------------------------------------------------------------------------- */
    /* getPrimarySiteId:                                                            */
    /* ---------------------------------------------------------------------------- */
    @Override
    public String getPrimarySiteId() {return _primarySiteId;}

    /* ---------------------------------------------------------------------------- */
    /* getPrimarySite:                                                              */
    /* ---------------------------------------------------------------------------- */
	@Override
	public Site getPrimarySite() {return _primarySite;}
	
    /* ---------------------------------------------------------------------------- */
    /* getSiteAdminTenantId:                                                        */
    /* ---------------------------------------------------------------------------- */
	@Override
	public String getSiteAdminTenantId(String siteId) 
	{return _sites.get(siteId).getSiteAdminTenantId();}
	
    /* **************************************************************************** */
    /*                               Private Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* getTenantsPath:                                                              */
    /* ---------------------------------------------------------------------------- */
    private String getTenantsPath() {return _tenantServiceBaseUrl + TENANTS_PATH;}
    
    /* ---------------------------------------------------------------------------- */
    /* allowRefresh:                                                                */
    /* ---------------------------------------------------------------------------- */
    /** Has the minimum time between tenant list refreshes expired?
     * 
     * @return true if refresh is allowed, false otherwise
     */
    private boolean allowRefresh()
    {
        // Don't allow too many refreshes in a row.
        if (_lastUpdateTime == null) return true;
        if (Instant.now().isAfter(_lastUpdateTime.plusSeconds(MIN_REFRESH_SECONDS)))
            return true;
        return false;
    }
    
    /* ---------------------------------------------------------------------------- */
    /* clear:                                                                       */
    /* ---------------------------------------------------------------------------- */
    /** Reset most fields to their initial null value. */
    private void clear()
    {
    	_tenants = null;
    	_sites = null;
    	_allowableTenants = null;
    	_primarySiteId = null;
    	_primarySite = null;
    }
    
    /* ---------------------------------------------------------------------------- */
    /* checkTenantMaps:                                                             */
    /* ---------------------------------------------------------------------------- */
    /** Basic consistency checking between references into the tenants and sites mappings.
     * Inconsistencies are simply logged for now.
     * 
     * @param tenants the tenants map
     * @param sites the sites map
     */
    private void checkTenantMaps(Map<String,Tenant> tenants, Map<String,Site> sites)
    {
    	// Assume no errors.
    	boolean noErrors = true;
    	
    	// Total number of primary sites.
    	var primarySiteIds = new ArrayList<String>();
    	
    	// --- Cycle through the tenants map.
    	for (var entry : tenants.entrySet()) {
    		// Make sure each tenant's site exists.
    		var site = sites.get(entry.getValue().getSiteId());
    		if (site == null) {
                String msg = MsgUtils.getMsg("TAPIS_TENANT_NO_SITE", entry.getKey(), 
                		                     entry.getValue().getSiteId());
                _log.error(msg);
                noErrors = false;
    		}
    	}
    	
    	// --- Cycle through the sites map.
    	for (var entry : sites.entrySet()) {
    		// Make sure every site has a admin tenant.
    		var adminTenant = tenants.get(entry.getValue().getSiteAdminTenantId());
    		if (adminTenant == null) {
                String msg = MsgUtils.getMsg("TAPIS_SITE_NO_ADMIN_TENANT", entry.getKey(), 
                		                     entry.getValue().getSiteAdminTenantId());
                _log.error(msg);
                noErrors = false;
    		}
    		
    		// Make sure there's only one primary site.
    		var isPrimary = entry.getValue().getPrimary();
    		if (isPrimary == null) {
                String msg = MsgUtils.getMsg("TAPIS_SITE_NO_PRIMARY_SETTING", entry.getKey()); 
                _log.error(msg);
                noErrors = false;
    		} else {
    			if (isPrimary) primarySiteIds.add(entry.getKey());
    		}
    	}
    	
    	// There should be exactly one primary site.
    	if (primarySiteIds.isEmpty()) {
    		String s = sites.keySet().stream().collect(Collectors.joining(", "));
            String msg = MsgUtils.getMsg("TAPIS_SITE_NO_PRIMARY", s); 
            _log.error(msg);
            noErrors = false;
    	} else if (primarySiteIds.size() > 1) {
    		String s = primarySiteIds.stream().collect(Collectors.joining(", "));
            String msg = MsgUtils.getMsg("TAPIS_SITE_MULTIPLE_PRIMARIES", s); 
            _log.error(msg);
            noErrors = false;
    	}
    	
    	// Informational message when no errors are detected.
    	if (noErrors) {
            String msg = MsgUtils.getMsg("TAPIS_TENANTS_CROSS_REFERENCED", 
            		                     tenants.size(), sites.size()); 
            _log.info(msg);
    	}
    }
    
    /* ---------------------------------------------------------------------------- */
    /* calculateAllowableTenants:                                                   */
    /* ---------------------------------------------------------------------------- */
    /** Create the mapping of site admin tenants to the list of tenants they may act
     * on behalf of.
     * 
     * @param tenants the tenants map
     * @param sites the sites map
     * @return the map of site admin tenants to their allowable tenants
     */
    private Map<String,List<String>> calculateAllowableTenants(Map<String,Tenant> tenants, 
    		                                                   Map<String,Site> sites)
    {
    	// Create a map with sufficient capacity. The key is a site admin tenant
    	// and the value is the list of tenant ids owned by the site admin.
    	var allowMap = new HashMap<String,List<String>>(1+sites.size()*2);
    	
    	// Create a temporary site to site admin tenant mapping.
    	var siteToSiteAdminTenant = new HashMap<String,String>(1+sites.size()*2);
    	
    	// Initialize allowMap with the site admin tenants as keys and an empty 
    	// tenants list as values.  Initialize the temporary map with sites as 
    	// keys and their admin tenants as values.  There better be only 1 
    	// primary site.
    	ArrayList<String> primarySiteList = null;
    	for (var entry : sites.entrySet()) {
    		String siteAdminTenant = entry.getValue().getSiteAdminTenantId();
    		var list = new ArrayList<String>();
    		allowMap.put(siteAdminTenant, list);
    		siteToSiteAdminTenant.put(entry.getKey(), siteAdminTenant);
    		if (entry.getValue().getPrimary()) {
    			_primarySiteId  = entry.getKey();
    			_primarySite    = entry.getValue();
    			primarySiteList = list;
    		}
    	}
    	
    	// Populate the allowMap's site admin entries. Inconsistent data 
    	// errors are ignored and must be fixed in the Tenants service database.
    	for (var entry : tenants.entrySet()) {
    		var siteId = entry.getValue().getSiteId();
    		if (siteId == null) continue;           // should never happen.
    		var siteAdminTenant = siteToSiteAdminTenant.get(siteId);
    		if (siteAdminTenant == null) continue; // should never happen
    		var list = allowMap.get(siteAdminTenant);
    		if (list == null) continue;             // should never happen.
    		list.add(entry.getKey());
    		
    		// Add every tenant to the primary site allowable list.
    		// If we didn't already just add the tenant to the list,
    		// do it now.
    		if (primarySiteList != list && primarySiteList != null)
    			primarySiteList.add(entry.getKey());
    	}
    	
    	return allowMap;
    }
    
    /* **************************************************************************** */
    /*                            RequestRoutingInfo Class                          */
    /* **************************************************************************** */
    public static class RequestRoutingInfo
    {
    	// All necessary routing information is contained within these fields.
    	private final String _tenant;
    	private final String _service;
    	private final String _baseUrl;
    	private final String _targetSiteId;
    	
    	// Constructor.
    	private RequestRoutingInfo(String tenant, String service, String baseUrl, 
    			                   String targetSiteId) 
          throws TapisException
    	{
    		// Assign input.
    		_tenant  = tenant;
    		_service = service;
    		_baseUrl = baseUrl;
    		_targetSiteId = targetSiteId;
    	}
    	
    	// Accessors.
    	public String getTenant() {return _tenant;}
		public String getService() {return _service;}
		public String getBaseUrl() {return _baseUrl;}
		public String getTargetSiteId() {return _targetSiteId;}
    }
}

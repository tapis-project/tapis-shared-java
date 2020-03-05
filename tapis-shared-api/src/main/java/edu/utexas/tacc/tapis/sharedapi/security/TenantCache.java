package edu.utexas.tacc.tapis.sharedapi.security;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.tenants.client.TenantsClient;
import edu.utexas.tacc.tapis.tenants.client.gen.model.Tenant;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.time.Duration;

public class TenantCache {

    private Duration REFRESH_TIME = Duration.ofSeconds(4 * 3600);
    private static LoadingCache<String, Tenant> cache;
    private static CacheLoader<String, Tenant> cacheLoader;

    @Inject
    public TenantCache(TenantsClient tenantsClient, @NotNull String baseUrl) throws TapisException {

        // Set the basePath on the client
        tenantsClient.getApiClient().setBasePath(baseUrl);

        // The cache will call this if the tenantId is not already in the cache, or when it refreshes.
        cacheLoader = new CacheLoader<String, Tenant>() {
            @Override
            public Tenant load(@NotNull String tenantId) throws Exception {
               return tenantsClient.getTenant(tenantId);
            }
        };

        cache = CacheBuilder.newBuilder()
                .refreshAfterWrite(REFRESH_TIME)
                .build(cacheLoader);

    }

    public LoadingCache<String, Tenant> getCache() {
        return cache;
    }





}

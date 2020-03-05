package edu.utexas.tacc.tapis.sharedapi.security;

import com.google.common.cache.*;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import edu.utexas.tacc.tapis.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.tokens.client.TokensClient;
import edu.utexas.tacc.tapis.tokens.client.gen.model.InlineObject1;
import edu.utexas.tacc.tapis.tokens.client.model.CreateTokenParms;
import edu.utexas.tacc.tapis.tokens.client.model.RefreshTokenParms;
import edu.utexas.tacc.tapis.tokens.client.model.TokenResponsePackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceJWTCache {

    private static final Logger log = LoggerFactory.getLogger(ServiceJWTCache.class);

    // REFRESH the cached token after this amount of time since writing
    private Duration REFRESH = Duration.of((long) 3 * 3600, ChronoUnit.SECONDS);

    // The actual expiration of the service token, set to expire one hour AFTER the refresh loop, just
    // in case the refresh failed for some reason.
    private Duration TOKEN_EXPIRATION = REFRESH.plusSeconds(3600);

    // TODO: Should this be a constant?
    private static final  String TOKENS_BASE_URL = "https://master.develop.tapis.io/";
    private static String SERVICE_NAME;
    private static CacheLoader<String, TokenResponsePackage> cacheLoader;
    private static LoadingCache<String, TokenResponsePackage> cache;
    private static TokensClient tokensClient;

    public static LoadingCache<String, TokenResponsePackage> getCache() {
        return cache;
    }
    private ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Inject
    public ServiceJWTCache(TokensClient tokClient, String password, String serviceName) {
        tokensClient = tokClient;
        tokensClient.getApiClient().setBasePath(TOKENS_BASE_URL);
        SERVICE_NAME = serviceName;

        cacheLoader = new CacheLoader<String, TokenResponsePackage>() {

            /**
             * load is called if there is no key in the cache, so for the first get, we need a password.
             * Once we have a token, we can refresh it with the refresh token.
             * @param tenantId
             * @return
             * @throws Exception
             */
            @Override
            public TokenResponsePackage load(String tenantId) throws Exception {
                return getTokenWithPassword(tenantId, password);
            }

            /**
             * reload is called when a key is evicted, and is async so that it doesn't block access until it
             * completes. The cache will still return the "old" key until this Future completes.
             * @param key
             * @param prevToken
             * @return
             */
            @Override
            public ListenableFuture<TokenResponsePackage> reload(final String key, TokenResponsePackage prevToken) {
                // asynchronous!
                ListenableFutureTask<TokenResponsePackage> task = ListenableFutureTask.create(new Callable<TokenResponsePackage>() {
                    public TokenResponsePackage call() throws TapisException {
                        return refreshToken(prevToken);
                    }
                });
                executorService.execute(task);
                return task;
            }
        };

        cache = CacheBuilder.newBuilder()
                .refreshAfterWrite(REFRESH)
                .build(cacheLoader);

    }


    private TokenResponsePackage refreshToken(TokenResponsePackage token) throws TapisException {
        RefreshTokenParms params = new RefreshTokenParms();
        params.setRefreshToken(token.getRefreshToken().getRefreshToken());
        TokenResponsePackage resp = tokensClient.refreshToken(params);
        return resp;
    }

    private TokenResponsePackage getTokenWithPassword(String tenantId, String password) throws TapisClientException {
        // Create and populate the client parameter object.
        var createParms = new CreateTokenParms();
        createParms.setTokenTenantId(tenantId);
        createParms.setTokenUsername(SERVICE_NAME);
        createParms.setAccountType(InlineObject1.AccountTypeEnum.SERVICE);
        createParms.setGenerateRefreshToken(true);
        createParms.setAccessTokenTtl((int) TOKEN_EXPIRATION.toSeconds());
        createParms.setRefreshTokenTtl((int) TOKEN_EXPIRATION.toSeconds());
        // Add basic auth header.
        String authString = SERVICE_NAME + ":" + password;
        String encodedString = Base64.getEncoder().encodeToString(authString.getBytes());
        tokensClient.addDefaultHeader("Authorization", "Basic " + encodedString);
        TokenResponsePackage resp = tokensClient.createToken(createParms);

        return resp;
    }

}

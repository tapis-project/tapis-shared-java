package edu.utexas.tacc.tapis.sharedapi.security;

import edu.utexas.tacc.tapis.tokens.client.TokensClient;
import edu.utexas.tacc.tapis.tokens.client.model.TapisAccessToken;
import edu.utexas.tacc.tapis.tokens.client.model.TapisRefreshToken;
import edu.utexas.tacc.tapis.tokens.client.model.TokenResponsePackage;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Test
public class TestServiceJWTCache {

    private TokensClient tokensClient = Mockito.mock(TokensClient.class, RETURNS_DEEP_STUBS);
    private TokenResponsePackage token;

    @BeforeTest
    public void setUp() {
        TapisAccessToken at = new TapisAccessToken();
        at.setAccessToken("test.access.token");
        TapisRefreshToken rt = new TapisRefreshToken();
        rt.setRefreshToken("test.refresh.token");
        token = new TokenResponsePackage();
        token.setRefreshToken(rt);
        token.setAccessToken(at);
    }


    @Test
    public void testCacheGetToken() throws Exception{
        ServiceJWTCache cache = new ServiceJWTCache(tokensClient, "testPassword", "test");
        when(tokensClient.getApiClient().setBasePath(any(String.class))).thenReturn(null);
        when(tokensClient.createToken(any())).thenReturn(token);
        TokenResponsePackage resp = cache.getCache().get("test");
        Assert.assertEquals("test.access.token", resp.getAccessToken().getAccessToken());
    }

    /**
     * The cache should automatically refresh the token on removal.
     * @throws Exception
     */
    @Test
    public void testCacheTokenRemoval() throws Exception{
        TapisAccessToken at = new TapisAccessToken();
        at.setAccessToken("test.access.token.refreshed");
        TapisRefreshToken rt = new TapisRefreshToken();
        rt.setRefreshToken("test.refreshed.token");
        TokenResponsePackage refreshed = new TokenResponsePackage();
        refreshed.setRefreshToken(rt);
        refreshed.setAccessToken(at);
        ServiceJWTCache cache = new ServiceJWTCache(tokensClient, "testPassword", "test");
        when(tokensClient.getApiClient().setBasePath(any(String.class))).thenReturn(null);
        when(tokensClient.createToken(any())).thenReturn(token);
        when(tokensClient.refreshToken(any())).thenReturn(refreshed);
        TokenResponsePackage r1 = cache.getCache().get("test");
        Assert.assertEquals("test.access.token", r1.getAccessToken().getAccessToken());

        //Refresh the cache, triggering the reload
        cache.getCache().refresh("test");
        //Have to sleep for a sec to let the async call happen.
        Thread.sleep(500);
        TokenResponsePackage resp = cache.getCache().get("test");
        Assert.assertEquals("test.access.token.refreshed", resp.getAccessToken().getAccessToken());
    }


}

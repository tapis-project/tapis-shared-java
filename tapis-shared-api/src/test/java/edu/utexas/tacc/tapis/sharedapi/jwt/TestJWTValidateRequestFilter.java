package edu.utexas.tacc.tapis.sharedapi.jwt;


import edu.utexas.tacc.tapis.shared.security.TenantManager;
import edu.utexas.tacc.tapis.sharedapi.jaxrs.filters.JWTValidateRequestFilter;
import edu.utexas.tacc.tapis.tenants.client.gen.model.Site;
import edu.utexas.tacc.tapis.tenants.client.gen.model.Tenant;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.JerseyTestNg;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import java.security.KeyPair;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Test
public class TestJWTValidateRequestFilter extends JerseyTestNg.ContainerPerClassTest {

    // setting up the mocks
    private Map<String, Tenant> tenantMap = new HashMap<>();
    private TenantManager tenantManager;
    private String signedJWT;
    private KeyPair keys;
    private Tenant tenant;
    private Site site;

    /*
    A really basic application for testing, just need something to trigger the JWT filter
     */
    @Path("/basic")
    public static class BasicTestsResource {

        @Path("test")
        @GET
        public String getTest() {
            return "ok";
        }
    }

    @BeforeClass
    void initTenantAndJWT() {
        keys = Keys.keyPairFor(SignatureAlgorithm.RS256);
        signedJWT = Jwts.builder()
            .setSubject("testUser")
            .setAudience("testAud")
            .claim("tapis/tenant_id", "testTenant")
            .claim("tapis/username", "testUser")
            .claim("tapis/token_type", "access")
            .claim("tapis/account_type",  "user")
            .signWith(keys.getPrivate())
            .compact();

        String str_key = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());

        tenant = new Tenant();
        tenant.setSiteId("localSite");
        tenant.setTenantId("testTenant");
        tenant.setBaseUrl("https://test.tapis.io");
        tenant.setPublicKey(str_key);
        tenantMap.put(tenant.getTenantId(), tenant);

        site = new Site();
        site.setSiteId("localSite");
    }


    @Override
    protected ResourceConfig configure() {

        tenantManager = Mockito.mock(TenantManager.class);

        JWTValidateRequestFilter.setSiteId("localSite");
        JWTValidateRequestFilter.setService("testService");
        JWTValidateRequestFilter filter = new JWTValidateRequestFilter(tenantManager);

        return new ResourceConfig()
            .register(BasicTestsResource.class)
            .register(filter);
    }

    @Test
    void testFilter() throws Exception {

        when(tenantManager.getTenants()).thenReturn(tenantMap);
        when(tenantManager.getTenant(any())).thenReturn(tenant);
        when(tenantManager.getSite(any())).thenReturn(site);

        Response r = target("basic/test")
            .request()
            .header("x-tapis-token", signedJWT)
            .get();
        Assert.assertEquals(r.getStatus(), 200);
    }

}

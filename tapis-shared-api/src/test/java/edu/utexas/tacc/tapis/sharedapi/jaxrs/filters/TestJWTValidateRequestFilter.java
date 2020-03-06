package edu.utexas.tacc.tapis.sharedapi.jaxrs.filters;


import edu.utexas.tacc.tapis.sharedapi.security.TenantCache;
import edu.utexas.tacc.tapis.tenants.client.gen.model.Tenant;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTestNg;
import org.mockito.*;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

@Test
public class TestJWTValidateRequestFilter extends JerseyTestNg.ContainerPerClassTest {

    private TenantCache tenantCache = Mockito.mock(TenantCache.class, RETURNS_DEEP_STUBS);
    private Tenant tenant;
    private PrivateKey privateKey;

    @Override
    protected ResourceConfig configure() {

        ResourceConfig conf = new ResourceConfig();
        conf.register(JWTValidateRequestFilter.class);
        conf.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(tenantCache).to(TenantCache.class);
            }
        });
        conf.register(TestApp.class);
        return conf;
    }

    @Path("/")
    public static class TestApp {

        @GET
        @Path("hasAuth")
        public Response hasAuth() {
            return Response.ok().build();
        }

        @GET
        @PermitAll
        @Path("permitAll")
        public Response permitAll() {
            return Response.ok().build();
        }

    }

    public String generateJwtToken(PrivateKey privateKey) {
        String token = Jwts.builder()
                .setSubject("test@master")
                .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
                .setIssuer("https://test.tapis.io/v3/tokens/")
                .claim("tapis/tenant_id", "testTenant")
                .claim("tapis/token_type", "access")
                .claim("tapis/delegation", false)
                .claim("tapis/delegation_sub", null)
                .claim("tapis/username", "testUser1")
                .claim("tapis/account_type", "user")
                .signWith(privateKey)
                .compact();
        return token;
    }

    @BeforeMethod
    private void init() throws Exception{
        KeyPair kp = Keys.keyPairFor(SignatureAlgorithm.RS256);
        PublicKey publicKey = kp.getPublic();
        List<String> allowedTenants = new ArrayList<>();
        allowedTenants.add("master");
        allowedTenants.add("testTenant");
        privateKey = kp.getPrivate();
        tenant = new Tenant();
        tenant.setBaseUrl("https://test.tapis.io");
        tenant.setTenantId("testTenant");
        tenant.setPublicKey(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
        tenant.setAllowableXTenantIds(allowedTenants);
    }


    @Test
    public void testNoJWT() {
        Response resp = target().path("/hasAuth").request().get(Response.class);
        Assert.assertEquals(resp.getStatus(), 401);
    }

    @Test
    public void testPermitAll() {
        Response resp = target().path("/permitAll").request().get(Response.class);
        Assert.assertEquals(resp.getStatus(), 200);
    }

    @Test
    public void testWithGoodJWT() throws Exception {
        when(tenantCache.getCache().get(any())).thenReturn(tenant);
        Response resp = target()
                .path("/hasAuth").request()
                .header("x-tapis-token", generateJwtToken(privateKey))
                .get(Response.class);
        Assert.assertEquals(resp.getStatus(), 200);
    }

    @Test
    public void testWithBadPublicKey() throws Exception {
        KeyPair kp = Keys.keyPairFor(SignatureAlgorithm.RS256);
        PublicKey pk = kp.getPublic();
        // setup the tenant to have a different pub key from what was used to sign the actual JWT
        tenant.setPublicKey(Base64.getEncoder().encodeToString(pk.getEncoded()));
        when(tenantCache.getCache().get(any())).thenReturn(tenant);
        Response resp = target()
                .path("/hasAuth").request()
                .header("x-tapis-token", generateJwtToken(privateKey))
                .get(Response.class);
        Assert.assertEquals(resp.getStatus(), 401);
    }

    @Test
    public void testWithNotAllowedTenant() throws Exception {

    }


}

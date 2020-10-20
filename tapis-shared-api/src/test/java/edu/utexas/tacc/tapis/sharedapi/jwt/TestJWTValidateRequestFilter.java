package edu.utexas.tacc.tapis.sharedapi.jwt;


import edu.utexas.tacc.tapis.sharedapi.jaxrs.filters.JWTValidateRequestFilter;
import edu.utexas.tacc.tapis.sharedapi.security.TenantManager;
import edu.utexas.tacc.tapis.tenants.client.gen.model.Tenant;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.JerseyTestNg;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

@Test
public class TestJWTValidateRequestFilter extends JerseyTestNg.ContainerPerClassTest {

    @Path("/basic")
    public static class BasicTestsResource {

        @Path("test")
        @GET
        public String getTest() {
            return "ok";
        }
    }

    @Override
    protected ResourceConfig configure() {

        TenantManager tenantManager = Mockito.mock(TenantManager.class);
        JWTValidateRequestFilter.setSiteId("testSite");
        JWTValidateRequestFilter.setService("testService");
        JWTValidateRequestFilter filter = new JWTValidateRequestFilter(tenantManager);
        return new ResourceConfig()
            .register(BasicTestsResource.class)
            .register(new JWTValidateRequestFilter(tenantManager));
    }


    @Test
    void testFilter() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setPublicKey();

        Response r = target("basic/test").request().get();
        Assert.assertEquals(r.getStatus(), 200);

    }

}

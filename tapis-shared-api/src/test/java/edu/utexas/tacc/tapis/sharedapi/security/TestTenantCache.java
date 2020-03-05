package edu.utexas.tacc.tapis.sharedapi.security;

import edu.utexas.tacc.tapis.tenants.client.TenantsClient;
import edu.utexas.tacc.tapis.tenants.client.gen.model.Tenant;
import edu.utexas.tacc.tapis.tokens.client.TokensClient;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Test
public class TestTenantCache {

    private TenantsClient tenantsClient;
    private Tenant testTenant;

    @BeforeMethod
    private void beforeMethod() {
        tenantsClient = Mockito.mock(TenantsClient.class, Mockito.RETURNS_DEEP_STUBS);
        testTenant = new Tenant();
        testTenant.setTenantId("testTenant");
        testTenant.setBaseUrl("https://test.io");
    }


    @Test
    public void testTenantLoad() throws Exception {
        when(tenantsClient.getApiClient().setBasePath(any())).thenReturn(null);
        when(tenantsClient.getTenant(any())).thenReturn(testTenant);
        TenantCache tc = new TenantCache(tenantsClient, "https://test.io");
        Tenant ten = tc.getCache().get("testTenant");
        Assert.assertEquals(ten.getTenantId(), testTenant.getTenantId());
    }

    @Test
    public void testTenantRefresh() throws Exception {
        when(tenantsClient.getApiClient().setBasePath(any())).thenReturn(null);
        when(tenantsClient.getTenant(any())).thenReturn(testTenant);
        TenantCache tc = new TenantCache(tenantsClient, "https://test.io");
        tc.getCache().get("testTenant");
        //Invalidate the key, which should trigger a load automaticallys
        tc.getCache().refresh("testTenant");
        // getTenant should get called 2x, one for the initial load, one for the refresh
        verify(tenantsClient, times(2)).getTenant(any());
    }

    @Test
    public void testGetTenantAfterInvalidate() throws Exception {
        when(tenantsClient.getApiClient().setBasePath(any())).thenReturn(null);
        when(tenantsClient.getTenant(any())).thenReturn(testTenant);
        TenantCache tc = new TenantCache(tenantsClient, "https://test.io");
        tc.getCache().get("testTenant");
        //Invalidate the key, which should trigger a load automatically
        tc.getCache().refresh("testTenant");
        // getTenant should get called 2x, one for the initial load, one for the refresh
        Tenant ten = tc.getCache().get("testTenant");
        Assert.assertEquals("testTenant", ten.getTenantId());
    }

}

package edu.utexas.tacc.tapis.shared.security;

import java.util.concurrent.ExecutionException;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import edu.utexas.tacc.tapis.apps.client.AppsClient;
import edu.utexas.tacc.tapis.auth.client.AuthClient;
import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.files.client.FilesClient;
import edu.utexas.tacc.tapis.meta.client.MetaClient;
import edu.utexas.tacc.tapis.security.client.SKClient;
import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.runtime.TapisRuntimeException;
import edu.utexas.tacc.tapis.systems.client.SystemsClient;
import edu.utexas.tacc.tapis.tenants.client.TenantsClient;
import edu.utexas.tacc.tapis.tokens.client.TokensClient;

/** This test is disabled to avoid storing a password in source code.
 * 
 * Three things need to be done to run this test:
 *  
 *  1. Assign the CALLING_SERVICE_PASSWORD to match the calling service.
 *  2. Set enabled = true on @BeforeSuite
 *  3. Set enabled = true on @Test
 * 
 * @author rcardone
 */
@Test(groups = {"integration"})
public class ServiceClientsTest 
{
    // Test configuration.
    private static final String TENANTS_BASE_URL = "https://admin.develop.tapis.io";
    private static final String SITE_ID = "tacc";
    private static final String CALLING_SERVICE_NAME = TapisConstants.SERVICE_NAME_JOBS;
    private static final String CALLING_SERVICE_PASSWORD = ""; // ASSIGN PASSWORD HERE
    
    // ENABLE THIS TO RUN
    @BeforeSuite(enabled = false)
    public void beforeSuite() 
     throws TapisRuntimeException, TapisClientException, TapisException
    {
        // Initialize tenants.
        String url = TENANTS_BASE_URL;
        var tenantMap = TenantManager.getInstance(url).getTenants();
     
        // Initialize the service context singleton which will get used by ServiceClients.
        ServiceContext serviceCxt = ServiceContext.getInstance();
        serviceCxt.initServiceJWT(SITE_ID, CALLING_SERVICE_NAME, CALLING_SERVICE_PASSWORD);
    }
    
    /** This test simply retrieves each tapis client object twice.  The first time the object
     * is created, the second time the cached object is returned.
     * 
     * ENABLE THIS TEST TO RUN
     * 
     * @throws RuntimeException
     * @throws TapisException
     * @throws ExecutionException
     */
    @Test(enabled = false)
    public void getAllClients() 
     throws RuntimeException, TapisException, ExecutionException
    {
        // Get clients for this user.
        String oboUser = "bozo";
        String oboTenant = "dev";
        
        // Create the clients.
        AppsClient apps        = ServiceClients.getInstance().getClient(oboUser, oboTenant, AppsClient.class);
        SKClient sk            = ServiceClients.getInstance().getClient(oboUser, oboTenant, SKClient.class);
        SystemsClient sys      = ServiceClients.getInstance().getClient(oboUser, oboTenant, SystemsClient.class);
        AuthClient authn       = ServiceClients.getInstance().getClient(oboUser, oboTenant, AuthClient.class);
        TenantsClient tenants  = ServiceClients.getInstance().getClient(oboUser, oboTenant, TenantsClient.class);
        TokensClient tokens    = ServiceClients.getInstance().getClient(oboUser, oboTenant, TokensClient.class);
        MetaClient meta        = ServiceClients.getInstance().getClient(oboUser, oboTenant, MetaClient.class);
        FilesClient files      = ServiceClients.getInstance().getClient(oboUser, oboTenant, FilesClient.class);

        // Get the cached clients.
        AppsClient apps2       = ServiceClients.getInstance().getClient(oboUser, oboTenant, AppsClient.class);
        SKClient sk2           = ServiceClients.getInstance().getClient(oboUser, oboTenant, SKClient.class);
        SystemsClient sys2     = ServiceClients.getInstance().getClient(oboUser, oboTenant, SystemsClient.class);
        AuthClient authn2      = ServiceClients.getInstance().getClient(oboUser, oboTenant, AuthClient.class);
        TenantsClient tenants2 = ServiceClients.getInstance().getClient(oboUser, oboTenant, TenantsClient.class);
        TokensClient tokens2   = ServiceClients.getInstance().getClient(oboUser, oboTenant, TokensClient.class);
        MetaClient meta2       = ServiceClients.getInstance().getClient(oboUser, oboTenant, MetaClient.class);
        FilesClient files2     = ServiceClients.getInstance().getClient(oboUser, oboTenant, FilesClient.class);
        
        // Make sure we only created one object of each client.
        Assert.assertEquals(apps == apps2, true);
        Assert.assertEquals(sk == sk2, true);
        Assert.assertEquals(sys == sys2, true);
        Assert.assertEquals(authn == authn2, true);
        Assert.assertEquals(tenants == tenants2, true);
        Assert.assertEquals(tokens == tokens2, true);
        Assert.assertEquals(meta == meta2, true);
        Assert.assertEquals(files == files2, true);
    }
}

package edu.utexas.tacc.tapis.shared.security.refresh;

import edu.utexas.tacc.tapis.shared.security.TenantManager;
import edu.utexas.tacc.tapis.systems.client.SystemsClient;

/** This is the driver program for MANUALLY testing token refresh across the 
 * security stack that includes ServiceJWT, ServiceContext and ServiceClients.  
 * The approach is for this class to:
 * 
 *    1. Simulate service code initialization.
 *    2. Use the MockServiceContext and MockServiceClients classes so that
 *       short timeout values are used to cause frequent token refreshes.
 *    3. Enter an infinite loop of calls to a service (Systems) using clients 
 *       cached in the MockServiceClient singleton.
 *       
 * The service calls should proceed without error across token refreshes.  The
 * console output will indicate when a token has been refreshed.  If the service
 * call fails the program will throw an exception and terminate.
 * 
 * Running TokenRefreshTest
 * ------------------------
 * 
 * This accepts 4 parameters and prints a help message if not all required 
 * parameters are provided.  The 4 command line parameters are, in order:
 * 
 *   1. Source service name
 *   2. Source service password
 *   3. Source site id
 *   4. Tenant baseurl (default = https://admin.develop.tapis.io)
 *   
 * These parameters allow one to simulate any service that makes call using 
 * clients cached by MockServiceClients, which is basically a copy of the
 * ServiceClients class that affects short refresh times.
 * 
 * This programs will run until manually terminated or an exception occurs.
 * 
 * @author rcardone
 */
public class TokenRefreshTest 
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Test values, change at will.
    private static final String TEST_USER   = "testuser2";
    private static final String TEST_TENANT = "dev";
    
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    private String _serviceName;
    private String _servicePassword;
    private String _siteId;
    private String _tenantBaseUrl = "https://admin.develop.tapis.io";

    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */

    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* main:                                                                  */
    /* ---------------------------------------------------------------------- */
    public static void main(String[] args) 
     throws Exception
    {
        var t = new TokenRefreshTest();
        t.execute(args);
    }
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* execute:                                                               */
    /* ---------------------------------------------------------------------- */
    private void execute(String[] args) 
     throws Exception
    {
        // User input.
        assignParameters(args);
        
        // Initialize tenants.
        var tenantMap = TenantManager.getInstance(_tenantBaseUrl).getTenants();
        
        // Create the service context singleton.
        MockServiceContext serviceCxt = MockServiceContext.getInstance();
        serviceCxt.initServiceJWT(_siteId, _serviceName, _servicePassword);
        
        // Print some information as a way of verifying the configuration.
        printSiteInfo();
        
        // Issue a client call.
        while (true) {
            // Get a client.
            var client = MockServiceClients.getInstance().getClient(
                           TEST_USER, TEST_TENANT, SystemsClient.class);
            
            // Use the client
            var list = client.getSystems();
            System.out.println("Number of systems returned: " + list.size());
            Thread.sleep(10000); // 10 seconds
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* assignParameters:                                                      */
    /* ---------------------------------------------------------------------- */
    private void assignParameters(String[] args)
    {
        // Check the parameters.
        if (args.length < 3) {
            String msg = "\nMISSING ARGUMENTS: " + 
                         getClass().getSimpleName() + " requires 4 parameters:\n\n" +
                         "   service name (jobs, systems, etc.)\n" +
                         "   service password\n" +
                         "   site id\n" +
                         "   tenant service baseurl (default = " + _tenantBaseUrl + "\n\n" +
                         "The first three parameters must be supplied on the command line.\n";
            throw new RuntimeException(msg);
        }
       
        // Assign required arguments.
        _serviceName = args[0];
        _servicePassword = args[1];
        _siteId = args[2];
       
        // Assign optional arguments.
        if (args.length > 3) _tenantBaseUrl = args[3];
    }
     
    /* ---------------------------------------------------------------------- */
    /* printSiteInfo:                                                      */
    /* ---------------------------------------------------------------------- */
    private void printSiteInfo()
    {
        System.out.println("--------------------------------");
        
        // Get the site information from the serviceJWT.
        MockServiceContext serviceCxt = MockServiceContext.getInstance();
        var targetSites = serviceCxt.getServiceJWT().getTargetSites();    
        int targetSiteCnt = targetSites != null ? targetSites.size() : 0;
        System.out.println("**** SUCCESS:  " + targetSiteCnt + " target sites retrieved ****");
        if (targetSites != null) {
            String s = "Target sites:\n";
            for (String site : targetSites) s += "  " + site + "\n";
            System.out.println(s);
        }
        
        System.out.println("--------------------------------\n");
    }
    
}

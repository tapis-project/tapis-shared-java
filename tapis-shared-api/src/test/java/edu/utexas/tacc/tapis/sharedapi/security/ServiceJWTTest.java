package edu.utexas.tacc.tapis.sharedapi.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;

@Test(groups= {"integration"})
public class ServiceJWTTest 
{
    /* ********************************************************************** */
    /*                              Constants                                 */
    /* ********************************************************************** */ 
    // Default settings are for the develop environment.  Change at will for
    // different environment and when values need to be modified.
    private static final String TOKENS_BASEURL = "https://dev.develop.tapis.io";
    private static final String TENANT  = "dev";
    private static final String SERVICE = "jobs";
    private static final String SERVICE_PWD = "3qLT0gy3MQrQKIiljEIRa2ieMEBIYMUyPSdYeNjIgZs=";
    private static final int    TTL_SECS = 30;
    private static final int    TEST_SECS = 3 * TTL_SECS + 10;
    
    /* ********************************************************************** */
    /*                              Tests                                     */
    /* ********************************************************************** */ 
    /* ---------------------------------------------------------------------- */
    /* refreshTokens:                                                         */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void refreshTokens() throws TapisException, InterruptedException
    { 
        // Set the token parms.
        var parms = new ServiceJWTParms();
        parms.setServiceName(SERVICE);
        parms.setTenant(TENANT);
        parms.setTokensBaseUrl(TOKENS_BASEURL);
        parms.setAccessTTL(TTL_SECS);
        parms.setRefreshTTL(TTL_SECS);
        
        // Create the service tokens.  This will refresh 
        // forever until shutdown.
        var serviceJwt = new ServiceJWT(parms, SERVICE_PWD);
        
        // Wait for several refresh cycles.
        Thread.currentThread().sleep(TEST_SECS * 1000);
        System.out.println("Completed " + TEST_SECS + " seconds wait.");
    }

}

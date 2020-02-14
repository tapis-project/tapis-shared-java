package edu.utexas.tacc.tapis.sharedapi.security;

import java.time.Instant;

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
    private static final int    ITERATIONS = 3;
    
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
        
        // Get start time.
        var start = Instant.now();
        
        // Create the service tokens.  This will refresh 
        // forever until shutdown.
        var serviceJwt = new ServiceJWT(parms, SERVICE_PWD);
        
        // Get each new JWT.
        for (int i = 0; i < ITERATIONS; i++) {
            System.out.println("JWT " + i + ": " + serviceJwt.getAccessJWT());
            Thread.sleep(TTL_SECS * 1000);
        }
        
        // Stop automatic refreshes.
        serviceJwt.interrupt();
        System.out.println("JWT " + ITERATIONS + ": " + serviceJwt.getAccessJWT());
        
        // Wait until the last JWT expires.
        System.out.println("Waiting for JWT expiration...");
        while (!serviceJwt.hasExpiredAccessJWT()) Thread.sleep(1000);
        
        // Calculate elapsed time in seconds.
        var stop = Instant.now();
        long elapsed = stop.minusSeconds(start.getEpochSecond()).getEpochSecond();
            
        // Wait for several refresh cycles.
        System.out.println("Completed in " + elapsed + " seconds.");
    }

}

package edu.utexas.tacc.tapis.sharedapi.security;

import java.time.Instant;
import java.util.List;

import edu.utexas.tacc.tapis.tokens.client.model.TokenResponsePackage;

public interface IServiceJWT 
{
    // Original inputs.
    String getServiceName();

    String getTenant();

    String getTokensBaseUrl();

    TokenResponsePackage getTokPkg(String targetSite);

    int getAccessTTL();

    int getRefreshTTL();

    String getDelegationTenant();

    String getDelegationUser();

    String getAdditionalClaims();
    
    List<String> getTargetSites();

    String getAccessJWT(String targetSite);

    Instant getAccessExpiresAt(String targetSite);

    // Seconds to expiration.  Negative means already expired.
    long getAccessExpiresIn(String targetSite);

    boolean hasExpiredAccessJWT(String targetSite);

    int getRefreshCount();

    Instant getLastRefreshTime();
    
    int getRefreshJwtCount();
    
    int getRefreshJwtFailedCount();
}
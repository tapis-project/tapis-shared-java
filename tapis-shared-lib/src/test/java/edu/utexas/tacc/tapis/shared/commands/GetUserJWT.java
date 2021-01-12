package edu.utexas.tacc.tapis.shared.commands;

import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.tokens.client.TokensClient;
import edu.utexas.tacc.tapis.tokens.client.gen.model.InlineObject1;
import edu.utexas.tacc.tapis.tokens.client.model.CreateTokenParms;
import edu.utexas.tacc.tapis.tokens.client.model.TapisAccessToken;

/** Generate a long-lived user JWT.  This program requires an authenticator JWT so that it
 * can have the permissions of an authenticator.  See GetAuthServiceJWT.java for a way
 * to get an authenticator JWT. 
 * 
 * @author rcardone
 */
public class GetUserJWT 
{
    private static final String BASE_URL   = "https://admin.develop.tapis.io";
    private static final String targetSite = "tacc";
    private static final String tenantId   = "dev"; 
    private static final String userName   = "testuser2";
    private static final int    accessTokenTTL = 60 * 60 * 24 * 365 * 10; // about 10 years in seconds
    
    // **** PUT AUTHENTICATOR JWT HERE ****
    private static final String authJWT = "";
    
    public static void main(String[] args) throws TapisClientException 
    {
        // Initialize service token request parms.
        CreateTokenParms parms = new CreateTokenParms();
        parms.accountType(InlineObject1.AccountTypeEnum.USER);
        parms.tokenTenantId(tenantId);
        parms.tokenUsername(userName);
        parms.setTargetSiteId(targetSite);
        parms.setAccessTokenTtl(accessTokenTTL);
        
        // Create the tokens client using basic auth.
        TokensClient tokensClt = new TokensClient(BASE_URL);
        tokensClt.getApiClient().addDefaultHeader("X-Tapis-Token", authJWT);
        tokensClt.getApiClient().addDefaultHeader("X-Tapis-Tenant", tenantId);
        tokensClt.getApiClient().addDefaultHeader("X-Tapis-User", userName);
        var resp = tokensClt.createToken(parms);
        
        TapisAccessToken token = resp.getAccessToken();
        System.out.println(token.getAccessToken());
    }
}

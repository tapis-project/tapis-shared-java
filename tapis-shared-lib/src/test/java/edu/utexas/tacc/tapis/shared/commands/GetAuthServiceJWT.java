package edu.utexas.tacc.tapis.shared.commands;

import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.tokens.client.TokensClient;
import edu.utexas.tacc.tapis.tokens.client.gen.model.InlineObject1;
import edu.utexas.tacc.tapis.tokens.client.model.CreateTokenParms;
import edu.utexas.tacc.tapis.tokens.client.model.TapisAccessToken;

/** Create a long-lived authenticator JWT.  This program requires an authenticator
 * password.  The JWT produced can be then be used by the GetUserJWT.java program
 * to generate user JWTs.
 * 
 * @author rcardone
 */
public class GetAuthServiceJWT 
{
    private static final String BASE_URL    = "https://admin.develop.tapis.io";
    private static final String targetSite  = "tacc";
    private static final String tenantId    = "admin"; 
    private static final String serviceName = "authenticator";
    private static final int accessTokenTTL = 60 * 60 * 24 * 365 * 10; // about 10 years in seconds
    
    // **** PUT PASSWORD HERE ****
    private static final String password = ""; 
    
    public static void main(String[] args) throws TapisClientException 
    {
        // Initialize service token request parms.
        CreateTokenParms parms = new CreateTokenParms();
        parms.accountType(InlineObject1.AccountTypeEnum.SERVICE);
        parms.tokenTenantId(tenantId);
        parms.tokenUsername(serviceName);
        parms.setTargetSiteId(targetSite);
        parms.setAccessTokenTtl(accessTokenTTL);
        
        // Create the tokens client using basic auth.
        TokensClient tokensClt = new TokensClient(BASE_URL, serviceName, password);
        var resp = tokensClt.createToken(parms);
        
        TapisAccessToken token = resp.getAccessToken();
        System.out.println(token.getAccessToken());
    }
}

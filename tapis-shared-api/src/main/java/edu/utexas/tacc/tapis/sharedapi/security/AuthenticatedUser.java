package edu.utexas.tacc.tapis.sharedapi.security;

import java.security.Principal;

/**
 *
 */
public class AuthenticatedUser implements Principal {

    private final String username;
    private final String tenantId;
    private final String accountType;
    private final String delegator;
    private final String serviceUser;
    private final String headerTenantId;
    private final String headerUserTokenHash;
    private final String jwt;

    public AuthenticatedUser(String username, String tenantId, String accountType, String delegator,
                             String serviceUser, String headerTenantId, String headerUserTokenHash, 
                             String jwt) 
    {
        this.username = username;
        this.tenantId = tenantId;
        this.accountType = accountType;
        this.delegator = delegator;
        this.serviceUser = serviceUser;
        this.headerTenantId = headerTenantId;
        this.headerUserTokenHash = headerUserTokenHash;
        this.jwt = jwt;
    }

    @Override
    public String getName() {
        return username;
    }

    public String getAccountType() { return accountType; }

    public String getDelegator() { return delegator; }

    public String getTenantId() {
        return tenantId;
    }

    public String getJwt() {
        return jwt;
    }

}


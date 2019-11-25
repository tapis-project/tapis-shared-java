package edu.utexas.tacc.tapis.sharedapi.security;

import io.jsonwebtoken.Jwt;

import java.security.Principal;

/**
 *
 */
public class AuthenticatedUser implements Principal {

    private final String username;
    private final String tenantId;
    private final String accountType;
    private final String delegator;
    private final String jwt;

    public AuthenticatedUser(String username, String tenantId, String accountType, String delegator, String jwt) {
        this.username = username;
        this.tenantId = tenantId;
        this.accountType = accountType;
        this.delegator = delegator;
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


package edu.utexas.tacc.tapis.sharedapi.security;


import io.jsonwebtoken.*;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import io.jsonwebtoken.security.Keys;

import java.security.KeyPair;

@Test
public class TestJWTValidator {

    private KeyPair keys;

    @BeforeClass
    public void setUpUsers() throws Exception {
        keys = Keys.keyPairFor(SignatureAlgorithm.RS256);
    }

    @Test
    void testGetClaimsNoValidation() {

        String jwts = Jwts.builder()
            .setSubject("testUser")
            .setAudience("testAud")
            .claim("tapis/tenant", "testTenant")
            .signWith(keys.getPrivate())
            .compact();

        TapisJWTValidator validator = new TapisJWTValidator(jwts);
        Claims claims = validator.getClaimsNoValidation();
        Assert.assertNotNull(claims);
        Assert.assertEquals(claims.getAudience(), "testAud");
        Assert.assertEquals(claims.get("tapis/tenant"), "testTenant");

    }


    /*
        Should throw a JwtException if it does not validate
        without the proper tapis claims.
     */
    @Test
    void testDoesValidateTapisClaims() {
        String jwts = Jwts.builder()
            .setSubject("testUser")
            .setAudience("testAud")
            .signWith(keys.getPrivate())
            .compact();
        TapisJWTValidator validator = new TapisJWTValidator(jwts);
        Assert.assertThrows(JwtException.class, ()->{validator.validate(keys.getPublic());});
    }


    @Test
    void testDoesValidateTapisClaimsValid() {
        String jwts = Jwts.builder()
            .claim("tapis/tenant_id", "testTenant")
            .claim("tapis/username", "testUser")
            .claim("tapis/token_type", "access")
            .claim("tapis/account_type",  "user")
            .claim("tapis/target_site", "localSite")
            .signWith(keys.getPrivate())
            .compact();
        TapisJWTValidator validator = new TapisJWTValidator(jwts);
        Jws<Claims> jws = validator.validate(keys.getPublic());
        Assert.assertNotNull(jws);
    }


}

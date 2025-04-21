// TODO
//      This class appears to be unused. Remove it.
//package edu.utexas.tacc.tapis.sharedapi.security;

import java.security.PublicKey;
import java.util.Objects;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

//public class TapisJWTValidator {
//
//    private String encodedJWT;
//
//    // Tapis claim keys.
//    private static final String CLAIM_TENANT = "tapis/tenant_id";
//    private static final String CLAIM_USERNAME = "tapis/username";
//    private static final String CLAIM_TOKEN_TYPE = "tapis/token_type";
//    private static final String CLAIM_ACCOUNT_TYPE = "tapis/account_type";
//    private static final String CLAIM_SITE = "tapis/target_site";
//
//    //These two are optional, only used when making a service request obo. If
//    // tapis/delegation is present, the tapis/delegation_sub claim is required
//    private static final String CLAIM_DELEGATION = "tapis/delegation";
//    private static final String CLAIM_DELEGATION_SUB = "tapis/delegation_sub";
//
//
//    public TapisJWTValidator(String encodedJWT) {
//        this.encodedJWT = encodedJWT;
//    }
//
//    public void validate(PublicKey publicKey) throws Exception {
//        var kwts = Jwts.parser()
//            .setSigningKey(publicKey)
//            .parseClaimsJws(encodedJWT);
//
//        Claims claims = jwts.getBody();
//        try {
//            Objects.requireNonNull(claims.get(CLAIM_TENANT));
//            Objects.requireNonNull(claims.get(CLAIM_USERNAME));
//            Objects.requireNonNull(claims.get(CLAIM_TOKEN_TYPE));
//            Objects.requireNonNull(claims.get(CLAIM_ACCOUNT_TYPE));
//            Objects.requireNonNull(claims.get(CLAIM_SITE));
//            if (claims.get(CLAIM_DELEGATION) != null) {
//                Objects.requireNonNull(claims.get(CLAIM_DELEGATION_SUB));
//            }
//        } catch (NullPointerException ex) {
//            throw new JwtException("Claims are not valid");
//        }
//        return jwts;
//
//    }
//
//    public Claims getClaimsNoValidation() {
//        int i = encodedJWT.lastIndexOf('.');
//        String withoutSignature = encodedJWT.substring(0, i+1);
//        Jwt<?,Claims> untrusted = Jwts.parser()
//            .parseClaimsJwt(withoutSignature);
//        return untrusted.getBody();
//    }
//
//
//
//
//}

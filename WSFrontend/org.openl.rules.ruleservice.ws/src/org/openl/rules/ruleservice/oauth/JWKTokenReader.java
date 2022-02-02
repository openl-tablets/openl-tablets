package org.openl.rules.ruleservice.oauth;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;

/**
 * Reader for jwk from jwt token.
 *
 * @author ybiruk
 */
public interface JWKTokenReader {
    
    JWK getJWK(SignedJWT jwt);
}

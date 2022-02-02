package org.openl.rules.ruleservice.oauth;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;

/**
 * Check JWT with JWK and validate following claims: exp, iss, aud.
 *
 * @author ybiruk
 */
@Component
public class JWTValidator {

    private List<JWKTokenReader> jwkReaders = Collections.emptyList();

    private Environment env;

    @Autowired
    public JWTValidator(Environment env){
        this.env = env;
    }

    @Autowired
    public void setJwkReaders(List<JWKTokenReader> jwkReaders) {
        this.jwkReaders = jwkReaders;
    }

    /**
     * Get jwk from url.
     * Url can be passed in token header or set in properties.
     *
     * @throws JOSEException If the RSA JWK extraction failed.
     * @throws ParseException If the string couldn't be parsed to a valid signed JWT.
     * @throws BadJWTException If validation fails for some other reason.
     */
    public void validateToken(String jwtToken) throws ParseException, JOSEException, BadJWTException {
        SignedJWT jwsToken = SignedJWT.parse(jwtToken.replaceFirst("Bearer", ""));
        JWK jwk = jwkReaders.stream()
            .map(r -> r.getJWK(jwsToken))
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow(() -> new JwtException("Unable to find jwk."));

        JWSVerifier verifier;
        KeyType keyType = jwk.getKeyType();
        if (KeyType.RSA.equals(keyType)) {
            verifier = new RSASSAVerifier(jwk.toRSAKey());
        } else if (KeyType.EC.equals(keyType)) {
            verifier = new ECDSAVerifier(jwk.toECKey());
        } else {
            throw new BadJWTException("Unsupported key type.");
        }

        if (!jwsToken.verify(verifier)) {
            throw new BadJWTException("Invalid key.");
        }

        DefaultJWTClaimsVerifier<SecurityContext> claimsVerifier = new DefaultJWTClaimsVerifier<>(

            // expected audience
            new HashSet<>(List.of(Objects.requireNonNull(env.getProperty("ruleservice.authentication.aud")))),

            // exact match claims
            null,

            // names of required claims
            new HashSet<>(List.of("exp", "iss")),

            // names of prohibited claims
            Collections.singleton("nonce"));
        claimsVerifier.verify(jwsToken.getJWTClaimsSet(), null);

        String issuers = env.getProperty("ruleservice.authentication.iss");
        String issuer = jwsToken.getJWTClaimsSet().getIssuer();
        if (issuers == null || Arrays.stream(issuers.split(",")).map(String::trim).noneMatch(el -> el.equals(issuer))) {
            throw new BadJWTException("Invalid issuer.");
        }
    }
}

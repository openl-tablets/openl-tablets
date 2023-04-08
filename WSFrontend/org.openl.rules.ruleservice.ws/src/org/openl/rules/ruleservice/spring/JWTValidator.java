package org.openl.rules.ruleservice.spring;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import org.openl.util.StringUtils;

/**
 * Check JWT with JWK and validate following claims: exp, iss, aud.
 *
 * @author Yury Molchan
 */
@Component
public class JWTValidator {

    private static final String BEARER = "Bearer ";

    private final Cache<String, JWK> keyCache = Cache2kBuilder.of(String.class, JWK.class)
            .entryCapacity(100)
            .expiryPolicy((k, v, loadTime, previous) -> {
                var d = v.getExpirationTime();
                return d == null ? System.currentTimeMillis() + 60 * 60 * 1000 : d.getTime(); // 1 hour
            })
            .sharpExpiry(true)
            .build();

    private final Environment env;

    @Autowired
    public JWTValidator(Environment env) {
        this.env = env;
    }

    public void validateToken(HttpServletRequest request) throws Exception {
        String pathInfo = request.getPathInfo();
        // Admin actions should be available without authorization.
        // Admin actions such as downloading or deploying via UI should be removed.
        if (pathInfo.startsWith("/admin/")) {
            return;
        }
        // Access to openapi.json and openapi.yam should pass without authorization.
        if (pathInfo.endsWith("openapi.json") || pathInfo.endsWith("openapi.yaml")) {
            return;
        }

        String jwtToken = request.getHeader("Authorization");

        if (jwtToken == null) {
            throw new IllegalArgumentException("Authorization header is not present.");
        }
        if (!jwtToken.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            throw new IllegalArgumentException("Bearer token is not present.");
        }

        SignedJWT jwsToken = SignedJWT.parse(jwtToken.substring(BEARER.length()));
        String keyID = jwsToken.getHeader().getKeyID();

        // Get public key
        JWK jwk = getJWK(keyID);
        if (jwk == null) {
            throw new BadJWTException("Unable to find JWK with id=" + keyID);
        }

        JWSVerifier verifier;
        var keyType = jwk.getKeyType();
        if (KeyType.RSA.equals(keyType)) {
            verifier = new RSASSAVerifier(jwk.toRSAKey());
        } else if (KeyType.EC.equals(keyType)) {
            verifier = new ECDSAVerifier(jwk.toECKey());
        } else {
            throw new BadJWTException("Unsupported key type for JWK with id=" + keyID);
        }

        if (!jwsToken.verify(verifier)) {
            throw new BadJWTException("Signature verification failed by JWK with id=" + keyID);
        }

        var claimsVerifier = new DefaultJWTClaimsVerifier<>(

                // expected audience
                new HashSet<>(List.of(env.getProperty("ruleservice.authentication.aud"))),

                // exact match claims
                null,

                // names of required claims
                new HashSet<>(List.of("exp", "iss")),

                // names of prohibited claims
                Collections.singleton("nonce"));
        claimsVerifier.verify(jwsToken.getJWTClaimsSet(), null);

        var issuer = jwsToken.getJWTClaimsSet().getIssuer();
        var issuers = StringUtils.split(env.getProperty("ruleservice.authentication.iss"), ',');
        if (issuers == null || !Arrays.asList(issuers).contains(issuer)) {
            throw new BadJWTException("Invalid issuer: " + issuer);
        }
    }

    private JWK getJWK(String keyID) throws Exception {
        if (keyID == null) {
            // No keyID -> no JWK
            return null;
        }
        JWK jwk = keyCache.get(keyID);
        if (jwk != null) {
            // The cache contains only actual not expired keys
            return jwk;
        }

        String jwkPropertyUrl = env.getProperty("ruleservice.authentication.jwks");
        if (StringUtils.isBlank(jwkPropertyUrl)) {
            throw new IllegalArgumentException("The 'ruleservice.authentication.jwks' property should contain valid URL");
        }

        // Update the cache by loading JWK public keys from the URL
        JWKSet jwkSet = JWKSet.load(new URL(jwkPropertyUrl).openStream());
        for (JWK key : jwkSet.getKeys()) {
            keyCache.put(key.getKeyID(), key);
        }

        return keyCache.get(keyID);
    }
}

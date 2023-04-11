package org.openl.rules.ruleservice.spring;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.nimbusds.jose.jwk.JWKSet;

import org.openl.rules.ruleservice.api.AuthorizationChecker;
import org.openl.spring.config.ConditionalOnEnable;
import org.openl.util.StringUtils;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;

import javax.servlet.http.HttpServletRequest;

/**
 * Check JWT with JWK and validate following claims: exp, iss, aud.
 *
 * @author Yury Molchan
 */
@Component
@ConditionalOnEnable("ruleservice.authentication.enabled")
public class JWTValidator implements AuthorizationChecker {

    private static final String BEARER = "Bearer ";

    private final Logger log = LoggerFactory.getLogger(JWTValidator.class);

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

    @Override
    public boolean authorize(HttpServletRequest httpRequest) {
        String pathInfo = httpRequest.getPathInfo();
        // Swagger and admin actions should be available without authorization.
        // Admin actions such as downloading or deploying via UI should be removed.
        if (pathInfo.startsWith("/admin/")) {
            return true;
        }
        // Access to openapi.json and openapi.yam should pass without authorization.
        if (pathInfo.endsWith("openapi.json") || pathInfo.endsWith("openapi.yaml")) {
            return true;
        }

        String jwtToken = httpRequest.getHeader("Authorization");
        if (jwtToken == null) {
            log.warn("Authorization header is not present.");
            return false;
        }
        if (!jwtToken.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            log.warn("Bearer token is not present.");
            return false;
        }

        try {
            SignedJWT jwsToken = SignedJWT.parse(jwtToken.substring(BEARER.length()));
            String keyID = jwsToken.getHeader().getKeyID();

            // Get public key
            JWK jwk = getJWK(keyID);
            if (jwk == null) {
                log.warn("Unable to find JWK with id={}.", keyID);
                return false;
            }

            JWSVerifier verifier;
            var keyType = jwk.getKeyType();
            if (KeyType.RSA.equals(keyType)) {
                verifier = new RSASSAVerifier(jwk.toRSAKey());
            } else if (KeyType.EC.equals(keyType)) {
                verifier = new ECDSAVerifier(jwk.toECKey());
            } else {
                log.warn("Unsupported key type for JWK with id={}.", keyID);
                return false;
            }

            if (!jwsToken.verify(verifier)) {
                log.warn("Signature verification failed by JWK with id={}.", keyID);
                return false;
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
                log.warn("Invalid '{}' issuer.", issuer);
                return false;
            }
        } catch (Exception e) {
            log.warn("Unexpected exception", e);
            return false;
        }

        return true;
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

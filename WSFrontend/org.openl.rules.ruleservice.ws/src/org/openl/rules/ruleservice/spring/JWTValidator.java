package org.openl.rules.ruleservice.spring;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import org.openl.rules.ruleservice.api.AuthorizationChecker;
import org.openl.spring.config.ConditionalOnEnable;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

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

    private final JwtConsumer jwtConsumer;

    @Autowired
    public JWTValidator(Environment env) throws JoseException, IOException {
        var expectedIssuers = StringUtils.split(env.getProperty("ruleservice.authentication.iss"), ',');
        if (CollectionUtils.isEmpty(expectedIssuers)) {
            throw new IllegalArgumentException("The 'ruleservice.authentication.iss' property should contain an issuer id");
        }

        var jwkPropertyUrl = env.getProperty("ruleservice.authentication.jwks");
        if (StringUtils.isEmpty(jwkPropertyUrl)) {
            throw new IllegalArgumentException("The 'ruleservice.authentication.jwks' property should contain valid URL");
        }

        VerificationKeyResolver jwksResolver;
        if (jwkPropertyUrl.startsWith("https:")) {
            // This resolver do secure connection, correctly handles "cache-control" header, do caching of the keys,
            // and multi-thread safe.
            jwksResolver = new HttpsJwksVerificationKeyResolver(new HttpsJwks(jwkPropertyUrl));
        } else {
            try (var resource = new URL(jwkPropertyUrl).openStream()) {
                var json = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
                var jwks = new JsonWebKeySet(json);
                jwksResolver = new JwksVerificationKeyResolver(jwks.getJsonWebKeys());
            }
        }

        var jwtConsumer = new JwtConsumerBuilder()
                .setRequireExpirationTime() // the JWT must have an expiration time
                .setAllowedClockSkewInSeconds(30) // allow some leeway in validating time based claims to account for clock skew
                .setExpectedIssuer(env.getProperty("ruleservice.authentication.iss")) // whom the JWT needs to have been issued by
                .setExpectedAudience(StringUtils.split(env.getProperty("ruleservice.authentication.aud"), ',')) // to whom the JWT is intended for
                .setVerificationKeyResolver(jwksResolver) // verify the signature with the public key
                .setJwsAlgorithmConstraints(AlgorithmConstraints.DISALLOW_NONE) // allow all algorithms
                .build(); // create the JwtConsumer instance

        this.jwtConsumer = jwtConsumer;
    }

    @Override
    public boolean authorize(HttpServletRequest httpRequest) {
        var pathInfo = httpRequest.getPathInfo();
        // Swagger and admin actions should be available without authorization.
        // Admin actions such as downloading or deploying via UI should be removed.
        if (pathInfo.startsWith("/admin/")) {
            return true;
        }
        // Access to openapi.json and openapi.yam should pass without authorization.
        if (pathInfo.endsWith("openapi.json") || pathInfo.endsWith("openapi.yaml")) {
            return true;
        }

        var credentials = httpRequest.getHeader("Authorization");
        if (credentials == null) {
            log.warn("Authorization header is not present.");
            return false;
        }
        if (!credentials.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            log.warn("Bearer token is not present.");
            return false;
        }

        try {
            //  Validate the JWT and process it to the Claims
            var jwtClaims = jwtConsumer.processToClaims(credentials.substring(BEARER.length()));
            log.info("Authorized for JWT ID={}", jwtClaims.getJwtId());
        } catch (Exception e) {
            log.warn("Unexpected exception", e);
            return false;
        }

        return true;
    }
}

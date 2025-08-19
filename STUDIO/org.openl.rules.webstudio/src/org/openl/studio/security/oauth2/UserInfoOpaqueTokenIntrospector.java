package org.openl.studio.security.oauth2;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.cache.Cache;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.core.converter.ClaimTypeConverter;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleUser;

/**
 * Extends {@link SpringOpaqueTokenIntrospector} to create a {@link SimpleUser} based on {@link OAuth2User}.
 */
public class UserInfoOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    private final OpaqueTokenIntrospector delegate;
    private final ClientRegistration clientRegistration;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> userService = new DefaultOAuth2UserService();
    private final Converter<Map<String, Object>, SimpleUser> userInfoClaimsConverter;
    private final PropertyResolver propertyResolver;
    private final Cache userInfoCache;

    private static final Converter<Map<String, Object>, Map<String, Object>> DEFAULT_CLAIM_TYPE_CONVERTER = new ClaimTypeConverter(
            OidcUserService.createDefaultClaimTypeConverters());

    public UserInfoOpaqueTokenIntrospector(String introspectionUri,
                                           ClientRegistration clientRegistration,
                                           Converter<Map<String, Object>, SimpleUser> userInfoClaimsConverter,
                                           PropertyResolver propertyResolver,
                                           Cache cache) {

        this.delegate = new SpringOpaqueTokenIntrospector(introspectionUri,
                clientRegistration.getClientId(),
                clientRegistration.getClientSecret());
        this.clientRegistration = clientRegistration;
        this.userInfoClaimsConverter = userInfoClaimsConverter;
        this.propertyResolver = propertyResolver;
        this.userInfoCache = cache;
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        var authorized = this.delegate.introspect(token);

        var tokenHash = getTokenHash(token);
        var userCacheValue = Optional.ofNullable(userInfoCache.get(tokenHash))
                .map(Cache.ValueWrapper::get)
                .map(UserCacheValue.class::cast)
                .orElse(null);
        if (userCacheValue == null) {
            var userRequest = new OAuth2UserRequest(clientRegistration, createAccessToken(token, authorized));
            var loadedUser = userService.loadUser(userRequest);
            var claims = Objects.requireNonNull(DEFAULT_CLAIM_TYPE_CONVERTER.convert(loadedUser.getAttributes()));
            var userInfo = Objects.requireNonNull(userInfoClaimsConverter.convert(claims));
            userCacheValue = new UserCacheValue(loadedUser.getAttributes(), userInfo.getAuthorities());
            userInfoCache.put(tokenHash, userCacheValue);
        }

        return new DefaultOAuth2User(userCacheValue.privileges,
                userCacheValue.userAttributes,
                propertyResolver.getProperty("security.oauth2.attribute.username"));
    }

    private OAuth2AccessToken createAccessToken(String token, OAuth2AuthenticatedPrincipal authorized) {
        Instant issuedAt = authorized.getAttribute(OAuth2TokenIntrospectionClaimNames.IAT);
        Instant expiresAt = authorized.getAttribute(OAuth2TokenIntrospectionClaimNames.EXP);
        return new OAuth2AccessToken(TokenType.BEARER, token, issuedAt, expiresAt);
    }

    private String getTokenHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Error hashing token", e);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static class UserCacheValue {

        public final Map<String, Object> userAttributes;
        public final Collection<Privilege> privileges;

        public UserCacheValue(Map<String, Object> userAttributes, Collection<Privilege> privileges) {
            this.userAttributes = Collections.unmodifiableMap(userAttributes);
            this.privileges = Collections.unmodifiableCollection(privileges);
        }
    }
}

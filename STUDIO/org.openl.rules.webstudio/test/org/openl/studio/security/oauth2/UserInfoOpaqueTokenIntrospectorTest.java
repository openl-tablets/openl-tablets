package org.openl.studio.security.oauth2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.core.convert.converter.Converter;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

import org.openl.rules.security.SimpleUser;

class UserInfoOpaqueTokenIntrospectorTest {

    private final OpaqueTokenIntrospector delegate = mock(OpaqueTokenIntrospector.class);
    @SuppressWarnings("unchecked")
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> userService = mock(OAuth2UserService.class);
    @SuppressWarnings("unchecked")
    private final Converter<Map<String, Object>, SimpleUser> claimsConverter = mock(Converter.class);
    private UserInfoOpaqueTokenIntrospector introspector;

    @BeforeEach
    void setUp() {
        var userInfo = Map.<String, Object>of("sub", "8fc84949", "preferred_username", "admin");
        when(userService.loadUser(any())).thenReturn(
                new DefaultOAuth2User(List.of(), userInfo, "preferred_username"));
        when(claimsConverter.convert(any())).thenReturn(SimpleUser.builder()
                .setUsername("admin")
                .setPrivileges(List.of(new SimpleGrantedAuthority("ADMIN")))
                .build());

        var environment = new MockEnvironment()
                .withProperty("security.oauth2.attribute.username", "preferred_username");
        introspector = new UserInfoOpaqueTokenIntrospector(delegate, userService, registration(), claimsConverter,
                environment, new ConcurrentMapCache("userInfoOAuth2Cache"));
    }

    @Test
    void thePrincipalCarriesTheSignInTheIntrospectionNames() {
        when(delegate.introspect("token")).thenReturn(introspected("sign-in-1"));

        var principal = introspector.introspect("token");

        assertEquals("admin", principal.getName());
        assertEquals("sign-in-1", principal.getAttribute("sid"));
        assertEquals(List.of(new SimpleGrantedAuthority("ADMIN")), List.copyOf(principal.getAuthorities()));
    }

    @Test
    void aTokenOfNoSignInLeavesThePrincipalWithoutOne() {
        when(delegate.introspect("service-token")).thenReturn(introspected(null));

        assertNull(introspector.introspect("service-token").getAttribute("sid"));
    }

    @Test
    void theUserInfoOfATokenIsAskedOnce() {
        when(delegate.introspect("token")).thenReturn(introspected("sign-in-1"));

        introspector.introspect("token");
        var again = introspector.introspect("token");

        verify(userService, times(1)).loadUser(any());
        assertEquals("sign-in-1", again.getAttribute("sid"));
    }

    private static DefaultOAuth2AuthenticatedPrincipal introspected(String signIn) {
        var attributes = new HashMap<String, Object>();
        attributes.put(OAuth2TokenIntrospectionClaimNames.ACTIVE, true);
        attributes.put(OAuth2TokenIntrospectionClaimNames.IAT, Instant.now());
        attributes.put(OAuth2TokenIntrospectionClaimNames.EXP, Instant.now().plusSeconds(300));
        if (signIn != null) {
            attributes.put("sid", signIn);
        }
        return new DefaultOAuth2AuthenticatedPrincipal("admin", attributes, List.of());
    }

    private static ClientRegistration registration() {
        return ClientRegistration.withRegistrationId("webstudio")
                .clientId("openlstudio")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/webstudio")
                .authorizationUri("http://keycloak/auth")
                .tokenUri("http://keycloak/token")
                .userInfoUri("http://keycloak/userinfo")
                .userNameAttributeName("preferred_username")
                .build();
    }
}

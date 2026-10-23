package org.openl.studio.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.openl.studio.audit.model.AuditAuthentication;
import org.openl.studio.security.pat.model.PatAuthenticationToken;
import org.openl.studio.transfer.TransferLinkAuthentication;

class AuthenticationMethodsTest {

    private static final Authentication PASSWORD = UsernamePasswordAuthenticationToken.authenticated("jdoe", null,
            List.of());

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void aTokenOrAnIdentityProviderNamesItself() {
        var oauth2User = new DefaultOAuth2User(List.of(), Map.of("sub", "jdoe"), "sub");
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("jdoe")
                .issuedAt(Instant.EPOCH)
                .expiresAt(Instant.EPOCH.plusSeconds(60))
                .build();

        assertEquals(AuditAuthentication.TOKEN, AuthenticationMethods.of(new PatAuthenticationToken("abc")));
        assertEquals(AuditAuthentication.BEARER, AuthenticationMethods.of(new BearerTokenAuthenticationToken("t")));
        assertEquals(AuditAuthentication.BEARER, AuthenticationMethods.of(new JwtAuthenticationToken(jwt)));
        assertEquals(AuditAuthentication.OAUTH2,
                AuthenticationMethods.of(new OAuth2AuthenticationToken(oauth2User, List.of(), "oidc")));
        assertEquals(AuditAuthentication.SAML, AuthenticationMethods.of(mock(Saml2Authentication.class)));
        assertEquals(AuditAuthentication.SAML, AuthenticationMethods.of(mock(Saml2AuthenticationToken.class)));
        assertEquals(AuditAuthentication.LINK,
                AuthenticationMethods.of(new TransferLinkAuthentication(
                        User.withUsername("jdoe").password("secret").build(), List.of())));
    }

    @Test
    void aPasswordSentWithTheRequestIsBasicAuthentication() {
        request().addHeader(HttpHeaders.AUTHORIZATION, "basic amRvZTpzZWNyZXQ=");

        assertEquals(AuditAuthentication.BASIC, AuthenticationMethods.of(PASSWORD));
    }

    @Test
    void aPasswordOfTheSessionIsTheLoginForm() {
        request();

        assertEquals(AuditAuthentication.FORM, AuthenticationMethods.of(PASSWORD));
    }

    @Test
    void aPasswordOfABrowserSessionIsTheLoginFormWhateverTheRequest() {
        var oauth2User = new DefaultOAuth2User(List.of(), Map.of("sub", "jdoe"), "sub");
        request().addHeader(HttpHeaders.AUTHORIZATION, "basic amRvZTpzZWNyZXQ=");

        assertEquals(AuditAuthentication.FORM, AuthenticationMethods.ofSession(PASSWORD));
        assertEquals(AuditAuthentication.TOKEN, AuthenticationMethods.ofSession(new PatAuthenticationToken("abc")));
        assertEquals(AuditAuthentication.OAUTH2,
                AuthenticationMethods.ofSession(new OAuth2AuthenticationToken(oauth2User, List.of(), "oidc")));
    }

    @Test
    void nothingIsToldWithoutAUserOrARequest() {
        assertNull(AuthenticationMethods.of(null));
        assertNull(AuthenticationMethods.of(new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"))));
        assertNull(AuthenticationMethods.of(PASSWORD));
        assertNull(AuthenticationMethods.of(mock(Authentication.class)));
    }

    private static MockHttpServletRequest request() {
        var request = new MockHttpServletRequest("GET", "/rest/projects");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }
}

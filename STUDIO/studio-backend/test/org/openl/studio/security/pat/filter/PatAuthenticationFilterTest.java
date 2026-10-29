package org.openl.studio.security.pat.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import org.openl.studio.security.pat.model.ExpiredPatException;
import org.openl.studio.security.pat.model.MalformedPatException;
import org.openl.studio.security.pat.model.PatAuthResolution;
import org.openl.studio.security.pat.model.PatAuthenticationToken;
import org.openl.studio.security.pat.model.PatRefusal;
import org.openl.studio.security.pat.model.PatToken;
import org.openl.studio.security.pat.model.UnknownPatException;
import org.openl.studio.security.pat.service.PatAuthService;

/**
 * Unit tests for {@link PatAuthenticationFilter}.
 * Tests PAT authentication filter logic using mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PatAuthenticationFilterTest {

    // Valid test tokens matching PatToken format requirements (16 char publicId, 32 char secret)
    private static final String TEST_PUBLIC_ID = "abc123DEF4567890"; // 16 Base62 chars
    private static final String TEST_SECRET = "secret456ABCDEF78901234567890ABA"; // 32 Base62 chars
    private static final String TEST_TOKEN_VALUE = "openl_pat_" + TEST_PUBLIC_ID + "." + TEST_SECRET;
    /** The answer to a token that is not valid, whatever the reason. */
    private static final String NOT_VALID = """
            The personal access token is not valid. Create a new token in Personal \
            Access Tokens of OpenL Studio and use it instead.""";

    @Mock
    private PatAuthService patAuthService;

    @Mock
    private SecurityContextHolderStrategy securityContextHolderStrategy;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private FilterChain filterChain;

    @Mock
    private AuthenticationEventPublisher eventPublisher;

    private PatAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        var messages = new ReloadableResourceBundleMessageSource();
        messages.setBasename("classpath:ValidationMessages");
        messages.setDefaultEncoding("UTF-8");
        filter = new PatAuthenticationFilter(patAuthService, eventPublisher, messages, new ObjectMapper(),
                securityContextHolderStrategy);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        // Default: no existing authentication
        when(securityContextHolderStrategy.getContext()).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);
        when(securityContextHolderStrategy.createEmptyContext()).thenReturn(securityContext);
    }

    @Test
    void testDoFilter_ValidToken() throws ServletException, IOException {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token " + TEST_TOKEN_VALUE);

        var userDetails = createUserDetails("jdoe", "ROLE_USER");
        var authentication = patToken(userDetails);
        PatAuthResolution resolution = PatAuthResolution.valid(authentication);

        when(patAuthService.resolveAuthentication(any(PatToken.class))).thenReturn(resolution);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(patAuthService, times(1)).resolveAuthentication(any(PatToken.class));
        verify(securityContextHolderStrategy, times(1)).createEmptyContext();
        verify(securityContext, times(1)).setAuthentication(authentication);
        verify(securityContextHolderStrategy, times(1)).setContext(securityContext);
        verify(filterChain, times(1)).doFilter(request, response);

        // Response should be successful (filter passed through)
        assertEquals(200, response.getStatus());
    }

    @Test
    void testDoFilter_NoAuthorizationHeader() throws ServletException, IOException {
        // Arrange - no Authorization header

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(patAuthService, never()).resolveAuthentication(any(PatToken.class));
        verify(securityContextHolderStrategy, never()).createEmptyContext();
        verify(filterChain, times(1)).doFilter(request, response);

        assertEquals(200, response.getStatus());
    }

    @Test
    void testDoFilter_AuthorizationHeaderWithoutTokenPrefix() throws ServletException, IOException {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer some-bearer-token");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(patAuthService, never()).resolveAuthentication(any(PatToken.class));
        verify(securityContextHolderStrategy, never()).createEmptyContext();
        verify(filterChain, times(1)).doFilter(request, response);

        assertEquals(200, response.getStatus());
    }

    @Test
    void testDoFilter_InvalidTokenFormat() throws ServletException, IOException {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token invalid-token-format");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(patAuthService, never()).resolveAuthentication(any(PatToken.class));
        verify(securityContextHolderStrategy, never()).createEmptyContext();
        verify(filterChain, never()).doFilter(request, response);

        assertRefusedWith("openl.error.401.token.refused.message", NOT_VALID);
        // A token that cannot be read is refused without naming a token.
        assertNull(refusedToken().getPublicId());
        assertInstanceOf(MalformedPatException.class, refusalReason());
    }

    @Test
    void testDoFilter_ValidFormatButInvalidToken() throws ServletException, IOException {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token " + TEST_TOKEN_VALUE);

        var wrongSecret = new BadCredentialsException("Wrong secret");
        PatAuthResolution resolution = PatAuthResolution.refused(new PatRefusal(wrongSecret, "jdoe"));

        when(patAuthService.resolveAuthentication(any(PatToken.class))).thenReturn(resolution);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(patAuthService, times(1)).resolveAuthentication(any(PatToken.class));
        verify(securityContextHolderStrategy, never()).createEmptyContext();
        verify(filterChain, never()).doFilter(request, response);

        // A wrong secret tells the caller no more than any other token that is not valid
        assertRefusedWith("openl.error.401.token.refused.message", NOT_VALID);
        // A refused token is published by its public id, never by its secret, with its reason and its owner.
        var refused = refusedToken();
        assertEquals(TEST_PUBLIC_ID, refused.getPublicId());
        assertFalse(refused.isAuthenticated());
        assertEquals("jdoe", refused.getName());
        assertSame(wrongSecret, refusalReason());
    }

    @Test
    void testDoFilter_RevokedToken() throws ServletException, IOException {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token " + TEST_TOKEN_VALUE);

        PatAuthResolution resolution = PatAuthResolution.refused(new PatRefusal(new UnknownPatException(), null));

        when(patAuthService.resolveAuthentication(any(PatToken.class))).thenReturn(resolution);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(patAuthService, times(1)).resolveAuthentication(any(PatToken.class));
        verify(securityContextHolderStrategy, never()).createEmptyContext();
        verify(filterChain, never()).doFilter(request, response);

        assertRefusedWith("openl.error.401.token.refused.message", NOT_VALID);
        assertEquals("", refusedToken().getName(), "An unknown token names no user");
    }

    static Stream<Arguments> reasonsTheHolderOfTheSecretLearns() {
        var userExpired = """
                The account or the password of the user of the personal access token has expired. Ask an \
                administrator of OpenL Studio to renew it.""";
        return Stream.of(
                Arguments.of(new ExpiredPatException(), "openl.error.401.token.expired.message",
                        """
                        The personal access token has expired. Create a new token in Personal Access Tokens of \
                        OpenL Studio and use it instead."""),
                Arguments.of(new DisabledException("Disabled"), "openl.error.401.token.user.disabled.message",
                        """
                        The user of the personal access token is disabled. Ask an administrator of OpenL Studio \
                        to enable the user."""),
                Arguments.of(new LockedException("Locked"), "openl.error.401.token.user.locked.message",
                        """
                        The user of the personal access token is locked. Ask an administrator of OpenL Studio to \
                        unlock the user."""),
                Arguments.of(new AccountExpiredException("Expired"), "openl.error.401.token.user.expired.message",
                        userExpired),
                Arguments.of(new CredentialsExpiredException("Expired"), "openl.error.401.token.user.expired.message",
                        userExpired));
    }

    @Test
    void answersATokenThatCannotBeCheckedAsAFailureOfTheServer() throws ServletException, IOException {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token " + TEST_TOKEN_VALUE);
        var failure = new IllegalStateException("The database is down");
        when(patAuthService.resolveAuthentication(any(PatToken.class))).thenThrow(failure);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertNull(response.getHeader(HttpHeaders.WWW_AUTHENTICATE), "Nothing tells to replace the token");
        assertEquals("", response.getContentAsString());
        // The audit log records the failure all the same
        var reason = assertInstanceOf(AuthenticationServiceException.class, refusalReason());
        assertSame(failure, reason.getCause());
        assertEquals(TEST_PUBLIC_ID, refusedToken().getPublicId());
    }

    @ParameterizedTest
    @MethodSource("reasonsTheHolderOfTheSecretLearns")
    void tellsTheHolderOfTheSecretWhyTheTokenIsRefused(AuthenticationException reason, String code, String message)
            throws ServletException, IOException {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token " + TEST_TOKEN_VALUE);
        when(patAuthService.resolveAuthentication(any(PatToken.class)))
                .thenReturn(PatAuthResolution.refused(new PatRefusal(reason, "jdoe")));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertRefusedWith(code, message);
        assertSame(reason, refusalReason());
        assertEquals("jdoe", refusedToken().getName());
    }

    @Test
    void testDoFilter_TokenWithWhitespace() throws ServletException, IOException {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token   " + TEST_TOKEN_VALUE + "   ");

        var userDetails = createUserDetails("jdoe", "ROLE_USER");
        var authentication = patToken(userDetails);
        PatAuthResolution resolution = PatAuthResolution.valid(authentication);

        when(patAuthService.resolveAuthentication(any(PatToken.class))).thenReturn(resolution);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(patAuthService, times(1)).resolveAuthentication(any(PatToken.class));
        verify(filterChain, times(1)).doFilter(request, response);

        assertEquals(200, response.getStatus());
    }

    @Test
    void testDoFilter_ExistingAuthenticationSameUser() throws ServletException, IOException {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token " + TEST_TOKEN_VALUE);

        // Existing authentication for jdoe
        var existingUserDetails = createUserDetails("jdoe", "ROLE_USER");
        var existingAuth = patToken(existingUserDetails);
        when(securityContext.getAuthentication()).thenReturn(existingAuth);

        // New PAT authentication also for jdoe
        var newUserDetails = createUserDetails("jdoe", "ROLE_USER");
        var newAuth = patToken(newUserDetails);
        PatAuthResolution resolution = PatAuthResolution.valid(newAuth);

        when(patAuthService.resolveAuthentication(any(PatToken.class))).thenReturn(resolution);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(patAuthService, times(1)).resolveAuthentication(any(PatToken.class));
        // Should NOT set authentication because same user is already authenticated
        verify(securityContextHolderStrategy, never()).createEmptyContext();
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain, times(1)).doFilter(request, response);

        assertEquals(200, response.getStatus());
    }

    @Test
    void testDoFilter_ExistingAuthenticationDifferentUser() throws ServletException, IOException {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token " + TEST_TOKEN_VALUE);

        // Existing authentication for jdoe
        var existingUserDetails = createUserDetails("jdoe", "ROLE_USER");
        var existingAuth = patToken(existingUserDetails);
        when(securityContext.getAuthentication()).thenReturn(existingAuth);

        // New PAT authentication for jsmith
        var newUserDetails = createUserDetails("jsmith", "ROLE_ADMIN");
        var newAuth = patToken(newUserDetails);
        PatAuthResolution resolution = PatAuthResolution.valid(newAuth);

        when(patAuthService.resolveAuthentication(any(PatToken.class))).thenReturn(resolution);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(patAuthService, times(1)).resolveAuthentication(any(PatToken.class));
        // Should set new authentication because different user
        verify(securityContextHolderStrategy, times(1)).createEmptyContext();
        verify(securityContext, times(1)).setAuthentication(newAuth);
        verify(securityContextHolderStrategy, times(1)).setContext(securityContext);
        verify(filterChain, times(1)).doFilter(request, response);

        assertEquals(200, response.getStatus());
    }

    @Test
    void testDoFilter_AnonymousAuthentication() throws ServletException, IOException {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token " + TEST_TOKEN_VALUE);

        // Existing anonymous authentication
        var anonymousAuth = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        when(securityContext.getAuthentication()).thenReturn(anonymousAuth);

        // New PAT authentication
        var newUserDetails = createUserDetails("jdoe", "ROLE_USER");
        var newAuth = patToken(newUserDetails);
        PatAuthResolution resolution = PatAuthResolution.valid(newAuth);

        when(patAuthService.resolveAuthentication(any(PatToken.class))).thenReturn(resolution);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(patAuthService, times(1)).resolveAuthentication(any(PatToken.class));
        // Should replace anonymous authentication with PAT authentication
        verify(securityContextHolderStrategy, times(1)).createEmptyContext();
        verify(securityContext, times(1)).setAuthentication(newAuth);
        verify(securityContextHolderStrategy, times(1)).setContext(securityContext);
        verify(filterChain, times(1)).doFilter(request, response);

        assertEquals(200, response.getStatus());
    }

    @Test
    void testDoFilter_UnauthenticatedExistingAuth() throws ServletException, IOException {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token " + TEST_TOKEN_VALUE);

        // Existing authentication but not authenticated
        Authentication existingAuth = mock(Authentication.class);
        when(existingAuth.isAuthenticated()).thenReturn(false);
        when(existingAuth.getName()).thenReturn("jdoe");
        when(securityContext.getAuthentication()).thenReturn(existingAuth);

        // New PAT authentication
        var newUserDetails = createUserDetails("jdoe", "ROLE_USER");
        var newAuth = patToken(newUserDetails);
        PatAuthResolution resolution = PatAuthResolution.valid(newAuth);

        when(patAuthService.resolveAuthentication(any(PatToken.class))).thenReturn(resolution);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(patAuthService, times(1)).resolveAuthentication(any(PatToken.class));
        // Should set authentication because existing auth is not authenticated
        verify(securityContextHolderStrategy, times(1)).createEmptyContext();
        verify(securityContext, times(1)).setAuthentication(newAuth);
        verify(securityContextHolderStrategy, times(1)).setContext(securityContext);
        verify(filterChain, times(1)).doFilter(request, response);

        assertEquals(200, response.getStatus());
    }

    @Test
    void testAuthenticationIsRequired_NoExistingAuth() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);

        // Act
        var required = filter.authenticationIsRequired("jdoe");

        // Assert
        assertTrue(required, "Authentication should be required when no existing auth");
    }

    @Test
    void testAuthenticationIsRequired_UnauthenticatedExistingAuth() {
        // Arrange
        Authentication existingAuth = mock(Authentication.class);
        when(existingAuth.isAuthenticated()).thenReturn(false);
        when(securityContext.getAuthentication()).thenReturn(existingAuth);

        // Act
        var required = filter.authenticationIsRequired("jdoe");

        // Assert
        assertTrue(required, "Authentication should be required when existing auth is not authenticated");
    }

    @Test
    void testAuthenticationIsRequired_AnonymousAuth() {
        // Arrange
        var anonymousAuth = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        when(securityContext.getAuthentication()).thenReturn(anonymousAuth);

        // Act
        var required = filter.authenticationIsRequired("jdoe");

        // Assert
        assertTrue(required, "Authentication should be required when existing auth is anonymous");
    }

    @Test
    void testAuthenticationIsRequired_SameUser() {
        // Arrange
        var userDetails = createUserDetails("jdoe", "ROLE_USER");
        var existingAuth = patToken(userDetails);
        when(securityContext.getAuthentication()).thenReturn(existingAuth);

        // Act
        var required = filter.authenticationIsRequired("jdoe");

        // Assert
        assertFalse(required, "Authentication should NOT be required when same user is already authenticated");
    }

    @Test
    void testAuthenticationIsRequired_DifferentUser() {
        // Arrange
        var userDetails = createUserDetails("jdoe", "ROLE_USER");
        var existingAuth = patToken(userDetails);
        when(securityContext.getAuthentication()).thenReturn(existingAuth);

        // Act
        var required = filter.authenticationIsRequired("jsmith");

        // Assert
        assertTrue(required, "Authentication should be required when different user");
    }

    @Test
    void testDoFilter_ParsedTokenPassedCorrectly() throws ServletException, IOException {
        // Arrange - Test that token is parsed correctly and passed to service
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token " + TEST_TOKEN_VALUE);

        var tokenCaptor = ArgumentCaptor.forClass(PatToken.class);

        var userDetails = createUserDetails("jdoe", "ROLE_USER");
        var authentication = patToken(userDetails);
        PatAuthResolution resolution = PatAuthResolution.valid(authentication);

        when(patAuthService.resolveAuthentication(any(PatToken.class))).thenReturn(resolution);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert - Verify the token was parsed and passed with correct publicId and secret
        verify(patAuthService).resolveAuthentication(tokenCaptor.capture());

        var capturedToken = tokenCaptor.getValue();
        assertEquals(TEST_PUBLIC_ID, capturedToken.publicId());
        assertEquals(TEST_SECRET, capturedToken.secret());
    }

    /** The authentication a valid personal access token of the user resolves to. */
    private static PatAuthenticationToken patToken(UserDetails userDetails) {
        return new PatAuthenticationToken(userDetails, null, userDetails.getAuthorities(), "abcdefghijklmnop");
    }

    /**
     * Helper method to create UserDetails with authorities.
     */
    private UserDetails createUserDetails(String username, String... authorities) {
        return new User(
                username,
                "password",
                Stream.of(authorities)
                        .map(SimpleGrantedAuthority::new)
                        .map(GrantedAuthority.class::cast)
                        .toList()
        );
    }

    private PatAuthenticationToken refusedToken() {
        var token = ArgumentCaptor.forClass(Authentication.class);
        verify(eventPublisher).publishAuthenticationFailure(any(AuthenticationException.class), token.capture());
        return (PatAuthenticationToken) token.getValue();
    }

    private AuthenticationException refusalReason() {
        var reason = ArgumentCaptor.forClass(AuthenticationException.class);
        verify(eventPublisher).publishAuthenticationFailure(reason.capture(), any(Authentication.class));
        return reason.getValue();
    }

    /** Checks the answer to a refused token: its code and its message, in the body and in the header. */
    private void assertRefusedWith(String code, String message) throws IOException {
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertEquals("Token error=\"invalid_token\", error_description=\"" + message + "\"",
                response.getHeader(HttpHeaders.WWW_AUTHENTICATE));
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        var body = new ObjectMapper().readTree(response.getContentAsString());
        assertEquals(code, body.get("code").asText());
        assertEquals(message, body.get("message").asText());
    }
}

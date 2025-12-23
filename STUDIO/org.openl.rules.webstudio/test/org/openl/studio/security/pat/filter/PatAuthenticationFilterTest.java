package org.openl.studio.security.pat.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import org.openl.studio.security.pat.model.PatAuthResolution;
import org.openl.studio.security.pat.model.PatAuthenticationToken;
import org.openl.studio.security.pat.model.PatToken;
import org.openl.studio.security.pat.service.PatAuthService;

/**
 * Unit tests for {@link PatAuthenticationFilter}.
 * Tests PAT authentication filter logic using mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PatAuthenticationFilterTest {

    // Valid test tokens matching PatToken format requirements (16 char publicId, 32 char secret)
    private static final String TEST_PUBLIC_ID = "abc123DEF4567890"; // 16 Base62 chars
    private static final String TEST_SECRET = "secret456ABCDEF78901234567890ABA"; // 32 Base62 chars
    private static final String TEST_TOKEN_VALUE = "openl_pat_" + TEST_PUBLIC_ID + "." + TEST_SECRET;

    @Mock
    private PatAuthService patAuthService;

    @Mock
    private SecurityContextHolderStrategy securityContextHolderStrategy;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private FilterChain filterChain;

    private PatAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    public void setUp() {
        filter = new PatAuthenticationFilter(patAuthService, securityContextHolderStrategy);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        // Default: no existing authentication
        when(securityContextHolderStrategy.getContext()).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);
        when(securityContextHolderStrategy.createEmptyContext()).thenReturn(securityContext);
    }

    @Test
    public void testDoFilter_ValidToken() throws ServletException, IOException {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token " + TEST_TOKEN_VALUE);

        UserDetails userDetails = createUserDetails("jdoe", "ROLE_USER");
        Authentication authentication = new PatAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        PatAuthResolution resolution = PatAuthResolution.valid(authentication);

        when(patAuthService.resolveAuthentication(any(PatToken.class))).thenReturn(resolution);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(patAuthService, times(1)).resolveAuthentication(any(PatToken.class));
        verify(securityContextHolderStrategy, times(1)).createEmptyContext();
        verify(securityContext, times(1)).setAuthentication(eq(authentication));
        verify(securityContextHolderStrategy, times(1)).setContext(eq(securityContext));
        verify(filterChain, times(1)).doFilter(request, response);

        // Response should be successful (filter passed through)
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDoFilter_NoAuthorizationHeader() throws ServletException, IOException {
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
    public void testDoFilter_AuthorizationHeaderWithoutTokenPrefix() throws ServletException, IOException {
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
    public void testDoFilter_InvalidTokenFormat() throws ServletException, IOException {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token invalid-token-format");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(patAuthService, never()).resolveAuthentication(any(PatToken.class));
        verify(securityContextHolderStrategy, never()).createEmptyContext();
        verify(filterChain, never()).doFilter(request, response);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertEquals("Unauthorized", response.getErrorMessage());
    }

    @Test
    public void testDoFilter_ValidFormatButInvalidToken() throws ServletException, IOException {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token " + TEST_TOKEN_VALUE);

        PatAuthResolution resolution = PatAuthResolution.invalid();

        when(patAuthService.resolveAuthentication(any(PatToken.class))).thenReturn(resolution);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(patAuthService, times(1)).resolveAuthentication(any(PatToken.class));
        verify(securityContextHolderStrategy, never()).createEmptyContext();
        verify(filterChain, never()).doFilter(request, response);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertEquals("Unauthorized", response.getErrorMessage());
    }

    @Test
    public void testDoFilter_ExpiredToken() throws ServletException, IOException {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token " + TEST_TOKEN_VALUE);

        PatAuthResolution resolution = PatAuthResolution.invalid();

        when(patAuthService.resolveAuthentication(any(PatToken.class))).thenReturn(resolution);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(patAuthService, times(1)).resolveAuthentication(any(PatToken.class));
        verify(securityContextHolderStrategy, never()).createEmptyContext();
        verify(filterChain, never()).doFilter(request, response);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertEquals("Unauthorized", response.getErrorMessage());
    }

    @Test
    public void testDoFilter_RevokedToken() throws ServletException, IOException {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token " + TEST_TOKEN_VALUE);

        PatAuthResolution resolution = PatAuthResolution.invalid();

        when(patAuthService.resolveAuthentication(any(PatToken.class))).thenReturn(resolution);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(patAuthService, times(1)).resolveAuthentication(any(PatToken.class));
        verify(securityContextHolderStrategy, never()).createEmptyContext();
        verify(filterChain, never()).doFilter(request, response);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    }

    @Test
    public void testDoFilter_TokenWithWhitespace() throws ServletException, IOException {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token   " + TEST_TOKEN_VALUE + "   ");

        UserDetails userDetails = createUserDetails("jdoe", "ROLE_USER");
        Authentication authentication = new PatAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
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
    public void testDoFilter_ExistingAuthenticationSameUser() throws ServletException, IOException {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token " + TEST_TOKEN_VALUE);

        // Existing authentication for jdoe
        UserDetails existingUserDetails = createUserDetails("jdoe", "ROLE_USER");
        Authentication existingAuth = new PatAuthenticationToken(
                existingUserDetails,
                null,
                existingUserDetails.getAuthorities()
        );
        when(securityContext.getAuthentication()).thenReturn(existingAuth);

        // New PAT authentication also for jdoe
        UserDetails newUserDetails = createUserDetails("jdoe", "ROLE_USER");
        Authentication newAuth = new PatAuthenticationToken(
                newUserDetails,
                null,
                newUserDetails.getAuthorities()
        );
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
    public void testDoFilter_ExistingAuthenticationDifferentUser() throws ServletException, IOException {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token " + TEST_TOKEN_VALUE);

        // Existing authentication for jdoe
        UserDetails existingUserDetails = createUserDetails("jdoe", "ROLE_USER");
        Authentication existingAuth = new PatAuthenticationToken(
                existingUserDetails,
                null,
                existingUserDetails.getAuthorities()
        );
        when(securityContext.getAuthentication()).thenReturn(existingAuth);

        // New PAT authentication for jsmith
        UserDetails newUserDetails = createUserDetails("jsmith", "ROLE_ADMIN");
        Authentication newAuth = new PatAuthenticationToken(
                newUserDetails,
                null,
                newUserDetails.getAuthorities()
        );
        PatAuthResolution resolution = PatAuthResolution.valid(newAuth);

        when(patAuthService.resolveAuthentication(any(PatToken.class))).thenReturn(resolution);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(patAuthService, times(1)).resolveAuthentication(any(PatToken.class));
        // Should set new authentication because different user
        verify(securityContextHolderStrategy, times(1)).createEmptyContext();
        verify(securityContext, times(1)).setAuthentication(eq(newAuth));
        verify(securityContextHolderStrategy, times(1)).setContext(eq(securityContext));
        verify(filterChain, times(1)).doFilter(request, response);

        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDoFilter_AnonymousAuthentication() throws ServletException, IOException {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token " + TEST_TOKEN_VALUE);

        // Existing anonymous authentication
        AnonymousAuthenticationToken anonymousAuth = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        when(securityContext.getAuthentication()).thenReturn(anonymousAuth);

        // New PAT authentication
        UserDetails newUserDetails = createUserDetails("jdoe", "ROLE_USER");
        Authentication newAuth = new PatAuthenticationToken(
                newUserDetails,
                null,
                newUserDetails.getAuthorities()
        );
        PatAuthResolution resolution = PatAuthResolution.valid(newAuth);

        when(patAuthService.resolveAuthentication(any(PatToken.class))).thenReturn(resolution);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(patAuthService, times(1)).resolveAuthentication(any(PatToken.class));
        // Should replace anonymous authentication with PAT authentication
        verify(securityContextHolderStrategy, times(1)).createEmptyContext();
        verify(securityContext, times(1)).setAuthentication(eq(newAuth));
        verify(securityContextHolderStrategy, times(1)).setContext(eq(securityContext));
        verify(filterChain, times(1)).doFilter(request, response);

        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDoFilter_UnauthenticatedExistingAuth() throws ServletException, IOException {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token " + TEST_TOKEN_VALUE);

        // Existing authentication but not authenticated
        Authentication existingAuth = mock(Authentication.class);
        when(existingAuth.isAuthenticated()).thenReturn(false);
        when(existingAuth.getName()).thenReturn("jdoe");
        when(securityContext.getAuthentication()).thenReturn(existingAuth);

        // New PAT authentication
        UserDetails newUserDetails = createUserDetails("jdoe", "ROLE_USER");
        Authentication newAuth = new PatAuthenticationToken(
                newUserDetails,
                null,
                newUserDetails.getAuthorities()
        );
        PatAuthResolution resolution = PatAuthResolution.valid(newAuth);

        when(patAuthService.resolveAuthentication(any(PatToken.class))).thenReturn(resolution);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(patAuthService, times(1)).resolveAuthentication(any(PatToken.class));
        // Should set authentication because existing auth is not authenticated
        verify(securityContextHolderStrategy, times(1)).createEmptyContext();
        verify(securityContext, times(1)).setAuthentication(eq(newAuth));
        verify(securityContextHolderStrategy, times(1)).setContext(eq(securityContext));
        verify(filterChain, times(1)).doFilter(request, response);

        assertEquals(200, response.getStatus());
    }

    @Test
    public void testAuthenticationIsRequired_NoExistingAuth() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);

        // Act
        boolean required = filter.authenticationIsRequired("jdoe");

        // Assert
        assertTrue(required, "Authentication should be required when no existing auth");
    }

    @Test
    public void testAuthenticationIsRequired_UnauthenticatedExistingAuth() {
        // Arrange
        Authentication existingAuth = mock(Authentication.class);
        when(existingAuth.isAuthenticated()).thenReturn(false);
        when(securityContext.getAuthentication()).thenReturn(existingAuth);

        // Act
        boolean required = filter.authenticationIsRequired("jdoe");

        // Assert
        assertTrue(required, "Authentication should be required when existing auth is not authenticated");
    }

    @Test
    public void testAuthenticationIsRequired_AnonymousAuth() {
        // Arrange
        AnonymousAuthenticationToken anonymousAuth = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        when(securityContext.getAuthentication()).thenReturn(anonymousAuth);

        // Act
        boolean required = filter.authenticationIsRequired("jdoe");

        // Assert
        assertTrue(required, "Authentication should be required when existing auth is anonymous");
    }

    @Test
    public void testAuthenticationIsRequired_SameUser() {
        // Arrange
        UserDetails userDetails = createUserDetails("jdoe", "ROLE_USER");
        Authentication existingAuth = new PatAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        when(securityContext.getAuthentication()).thenReturn(existingAuth);

        // Act
        boolean required = filter.authenticationIsRequired("jdoe");

        // Assert
        assertFalse(required, "Authentication should NOT be required when same user is already authenticated");
    }

    @Test
    public void testAuthenticationIsRequired_DifferentUser() {
        // Arrange
        UserDetails userDetails = createUserDetails("jdoe", "ROLE_USER");
        Authentication existingAuth = new PatAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        when(securityContext.getAuthentication()).thenReturn(existingAuth);

        // Act
        boolean required = filter.authenticationIsRequired("jsmith");

        // Assert
        assertTrue(required, "Authentication should be required when different user");
    }

    @Test
    public void testDoFilter_ParsedTokenPassedCorrectly() throws ServletException, IOException {
        // Arrange - Test that token is parsed correctly and passed to service
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token " + TEST_TOKEN_VALUE);

        ArgumentCaptor<PatToken> tokenCaptor = ArgumentCaptor.forClass(PatToken.class);

        UserDetails userDetails = createUserDetails("jdoe", "ROLE_USER");
        Authentication authentication = new PatAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        PatAuthResolution resolution = PatAuthResolution.valid(authentication);

        when(patAuthService.resolveAuthentication(any(PatToken.class))).thenReturn(resolution);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert - Verify the token was parsed and passed with correct publicId and secret
        verify(patAuthService).resolveAuthentication(tokenCaptor.capture());

        PatToken capturedToken = tokenCaptor.getValue();
        assertEquals(TEST_PUBLIC_ID, capturedToken.publicId());
        assertEquals(TEST_SECRET, capturedToken.secret());
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
                        .map(a -> (org.springframework.security.core.GrantedAuthority) a)
                        .toList()
        );
    }
}

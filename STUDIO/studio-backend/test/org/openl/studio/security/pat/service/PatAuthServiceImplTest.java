package org.openl.studio.security.pat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.openl.rules.security.standalone.persistence.PersonalAccessToken;
import org.openl.studio.security.pat.model.PatRefusal;
import org.openl.studio.security.pat.model.PatToken;
import org.openl.studio.security.pat.model.PatValidationResult;
import org.openl.studio.security.pat.model.UnknownPatException;

/**
 * Unit tests for {@link PatAuthServiceImpl}.
 * Tests authentication resolution logic using mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class PatAuthServiceImplTest {

    // Valid test tokens matching PatToken format requirements (16 char publicId, 32 char secret)
    private static final String TEST_PUBLIC_ID = "publicId12345678"; // 16 Base62 chars
    private static final String TEST_SECRET = "secretABCDEF123456789012345678"; // 32 Base62 chars
    private static final String TEST_PUBLIC_ID_2 = "publicId2ABCDEFG"; // 16 Base62 chars
    private static final String TEST_SECRET_2 = "secret2ABCDEFG1234567890123456"; // 32 Base62 chars

    @Mock
    private PatValidationService validator;

    @Mock
    private UserDetailsService userDetailsService;

    private PatAuthService authService;

    @BeforeEach
    void setUp() {
        authService = new PatAuthServiceImpl(validator, userDetailsService);
    }

    @Test
    void testResolveAuthentication_ValidToken() {
        // Arrange
        var patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);
        var storedToken = createPersonalAccessToken(TEST_PUBLIC_ID, "jdoe");
        PatValidationResult validResult = PatValidationResult.valid(storedToken);

        var userDetails = createUserDetails("jdoe", "ROLE_USER", "ROLE_ADMIN");

        when(validator.validate(patToken)).thenReturn(validResult);
        when(userDetailsService.loadUserByUsername("jdoe")).thenReturn(userDetails);

        // Act
        var resolution = authService.resolveAuthentication(patToken);

        // Assert
        assertTrue(resolution.valid());
        assertNotNull(resolution.authentication());

        var auth = resolution.authentication();
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, auth);
        assertSame(userDetails, auth.getPrincipal());
        assertNull(auth.getCredentials()); // Credentials should be null
        assertEquals(2, auth.getAuthorities().size());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> "ROLE_USER".equals(a.getAuthority())));
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));

        verify(validator, times(1)).validate(patToken);
        verify(userDetailsService, times(1)).loadUserByUsername("jdoe");
    }

    @Test
    void testResolveAuthentication_InvalidToken() {
        // Arrange
        var patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);
        PatValidationResult invalidResult = PatValidationResult.refused(new UnknownPatException(), null);

        when(validator.validate(patToken)).thenReturn(invalidResult);

        // Act
        var resolution = authService.resolveAuthentication(patToken);

        // Assert
        assertFalse(resolution.valid());
        assertNull(resolution.authentication());
        assertSame(invalidResult.refusal(), resolution.refusal(), "The reason of the validation is kept");

        verify(validator, times(1)).validate(patToken);
        verify(userDetailsService, never()).loadUserByUsername("jdoe");
    }

    static Stream<Arguments> ownersThatCannotSignIn() {
        return Stream.of(
                Arguments.of(User.withUsername("jdoe").password("x").disabled(true).build(), DisabledException.class),
                Arguments.of(User.withUsername("jdoe").password("x").accountLocked(true).build(),
                        LockedException.class),
                Arguments.of(User.withUsername("jdoe").password("x").accountExpired(true).build(),
                        AccountExpiredException.class),
                Arguments.of(User.withUsername("jdoe").password("x").credentialsExpired(true).build(),
                        CredentialsExpiredException.class));
    }

    @ParameterizedTest
    @MethodSource("ownersThatCannotSignIn")
    void refusesTheTokenOfAnOwnerThatCannotSignInForThatReason(UserDetails owner, Class<?> reason) {
        var patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);
        when(validator.validate(patToken))
                .thenReturn(PatValidationResult.valid(createPersonalAccessToken(TEST_PUBLIC_ID, "jdoe")));
        when(userDetailsService.loadUserByUsername("jdoe")).thenReturn(owner);

        var resolution = authService.resolveAuthentication(patToken);

        assertFalse(resolution.valid());
        assertNull(resolution.authentication());
        var refusal = assertInstanceOf(PatRefusal.class, resolution.refusal());
        assertEquals(reason, refusal.reason().getClass());
        assertEquals("jdoe", refusal.owner());
    }


    @Test
    void testResolveAuthentication_ValidToken_UserNotFound() {
        // Arrange
        var patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);
        var storedToken = createPersonalAccessToken(TEST_PUBLIC_ID, "jdoe");
        PatValidationResult validResult = PatValidationResult.valid(storedToken);

        when(validator.validate(patToken)).thenReturn(validResult);
        var notFound = new UsernameNotFoundException("User not found");
        when(userDetailsService.loadUserByUsername("jdoe")).thenThrow(notFound);

        // Act
        var resolution = authService.resolveAuthentication(patToken);

        // Assert
        assertNull(resolution.authentication());
        var refusal = assertInstanceOf(PatRefusal.class, resolution.refusal());
        assertSame(notFound, refusal.reason());
        assertEquals("jdoe", refusal.owner());

        verify(validator, times(1)).validate(patToken);
        verify(userDetailsService, times(1)).loadUserByUsername("jdoe");
    }

    @Test
    void leavesAFailureToReadTheOwnerToTheCaller() {
        var patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);
        when(validator.validate(patToken))
                .thenReturn(PatValidationResult.valid(createPersonalAccessToken(TEST_PUBLIC_ID, "jdoe")));
        var failure = new IllegalStateException("The database is down");
        when(userDetailsService.loadUserByUsername("jdoe")).thenThrow(failure);

        var thrown = assertThrows(IllegalStateException.class, () -> authService.resolveAuthentication(patToken));

        assertSame(failure, thrown);
    }

    @Test
    void testResolveAuthentication_UserWithoutAuthorities() {
        // Arrange
        var patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);
        var storedToken = createPersonalAccessToken(TEST_PUBLIC_ID, "jdoe");
        PatValidationResult validResult = PatValidationResult.valid(storedToken);

        var userDetails = createUserDetails("jdoe"); // No authorities

        when(validator.validate(patToken)).thenReturn(validResult);
        when(userDetailsService.loadUserByUsername("jdoe")).thenReturn(userDetails);

        // Act
        var resolution = authService.resolveAuthentication(patToken);

        // Assert
        assertTrue(resolution.valid());
        assertNotNull(resolution.authentication());

        var auth = resolution.authentication();
        assertTrue(auth.getAuthorities().isEmpty());

        verify(validator, times(1)).validate(patToken);
        verify(userDetailsService, times(1)).loadUserByUsername("jdoe");
    }

    @Test
    void testResolveAuthentication_UserWithSingleAuthority() {
        // Arrange
        var patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);
        var storedToken = createPersonalAccessToken(TEST_PUBLIC_ID, "jdoe");
        PatValidationResult validResult = PatValidationResult.valid(storedToken);

        var userDetails = createUserDetails("jdoe", "ROLE_VIEWER");

        when(validator.validate(patToken)).thenReturn(validResult);
        when(userDetailsService.loadUserByUsername("jdoe")).thenReturn(userDetails);

        // Act
        var resolution = authService.resolveAuthentication(patToken);

        // Assert
        assertTrue(resolution.valid());
        assertNotNull(resolution.authentication());

        var auth = resolution.authentication();
        assertEquals(1, auth.getAuthorities().size());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_VIEWER".equals(a.getAuthority())));

        verify(validator, times(1)).validate(patToken);
        verify(userDetailsService, times(1)).loadUserByUsername("jdoe");
    }

    @Test
    void testResolveAuthentication_MultipleTokensDifferentUsers() {
        // Arrange
        // First token for jdoe
        var patToken1 = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);
        var storedToken1 = createPersonalAccessToken(TEST_PUBLIC_ID, "jdoe");
        PatValidationResult validResult1 = PatValidationResult.valid(storedToken1);
        var userDetails1 = createUserDetails("jdoe", "ROLE_USER");

        // Second token for jsmith
        var patToken2 = new PatToken(TEST_PUBLIC_ID_2, TEST_SECRET_2);
        var storedToken2 = createPersonalAccessToken(TEST_PUBLIC_ID_2, "jsmith");
        PatValidationResult validResult2 = PatValidationResult.valid(storedToken2);
        var userDetails2 = createUserDetails("jsmith", "ROLE_ADMIN");

        when(validator.validate(patToken1)).thenReturn(validResult1);
        when(validator.validate(patToken2)).thenReturn(validResult2);
        when(userDetailsService.loadUserByUsername("jdoe")).thenReturn(userDetails1);
        when(userDetailsService.loadUserByUsername("jsmith")).thenReturn(userDetails2);

        // Act
        var resolution1 = authService.resolveAuthentication(patToken1);
        var resolution2 = authService.resolveAuthentication(patToken2);

        // Assert
        assertTrue(resolution1.valid());
        assertEquals("jdoe", ((UserDetails) resolution1.authentication().getPrincipal()).getUsername());
        assertTrue(resolution1.authentication().getAuthorities().stream()
                .anyMatch(a -> "ROLE_USER".equals(a.getAuthority())));

        assertTrue(resolution2.valid());
        assertEquals("jsmith", ((UserDetails) resolution2.authentication().getPrincipal()).getUsername());
        assertTrue(resolution2.authentication().getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));

        verify(validator, times(1)).validate(patToken1);
        verify(validator, times(1)).validate(patToken2);
        verify(userDetailsService, times(1)).loadUserByUsername("jdoe");
        verify(userDetailsService, times(1)).loadUserByUsername("jsmith");
    }

    @Test
    void testResolveAuthentication_AuthenticationIsAuthenticated() {
        // Arrange
        var patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);
        var storedToken = createPersonalAccessToken(TEST_PUBLIC_ID, "jdoe");
        PatValidationResult validResult = PatValidationResult.valid(storedToken);
        var userDetails = createUserDetails("jdoe", "ROLE_USER");

        when(validator.validate(patToken)).thenReturn(validResult);
        when(userDetailsService.loadUserByUsername("jdoe")).thenReturn(userDetails);

        // Act
        var resolution = authService.resolveAuthentication(patToken);

        // Assert
        assertTrue(resolution.valid());
        assertNotNull(resolution.authentication());

        // UsernamePasswordAuthenticationToken with principal and authorities is considered authenticated
        var auth = resolution.authentication();
        assertTrue(auth.isAuthenticated(), "Authentication should be marked as authenticated");

        verify(validator, times(1)).validate(patToken);
        verify(userDetailsService, times(1)).loadUserByUsername("jdoe");
    }

    @Test
    void testResolveAuthentication_VerifyAuthenticationStructure() {
        // Arrange
        var patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);
        var storedToken = createPersonalAccessToken(TEST_PUBLIC_ID, "jdoe");
        PatValidationResult validResult = PatValidationResult.valid(storedToken);
        var userDetails = createUserDetails("jdoe", "ROLE_USER", "ROLE_ADMIN");

        when(validator.validate(patToken)).thenReturn(validResult);
        when(userDetailsService.loadUserByUsername("jdoe")).thenReturn(userDetails);

        // Act
        var resolution = authService.resolveAuthentication(patToken);

        // Assert
        assertTrue(resolution.valid());
        var auth = resolution.authentication();

        // Verify structure
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, auth);
        assertEquals(userDetails, auth.getPrincipal());
        assertNull(auth.getCredentials());

        // Verify authorities match (not strict collection equality)
        assertEquals(2, auth.getAuthorities().size());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> "ROLE_USER".equals(a.getAuthority())));
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));

        verify(validator, times(1)).validate(patToken);
        verify(userDetailsService, times(1)).loadUserByUsername("jdoe");
    }

    @Test
    void testResolveAuthentication_SameTokenMultipleTimes() {
        // Arrange
        var patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);
        var storedToken = createPersonalAccessToken(TEST_PUBLIC_ID, "jdoe");
        PatValidationResult validResult = PatValidationResult.valid(storedToken);
        var userDetails = createUserDetails("jdoe", "ROLE_USER");

        when(validator.validate(patToken)).thenReturn(validResult);
        when(userDetailsService.loadUserByUsername("jdoe")).thenReturn(userDetails);

        // Act - call multiple times with same token
        var resolution1 = authService.resolveAuthentication(patToken);
        var resolution2 = authService.resolveAuthentication(patToken);

        // Assert - both should succeed
        assertTrue(resolution1.valid());
        assertTrue(resolution2.valid());
        assertEquals("jdoe", ((UserDetails) resolution1.authentication().getPrincipal()).getUsername());
        assertEquals("jdoe", ((UserDetails) resolution2.authentication().getPrincipal()).getUsername());

        // Verify called twice
        verify(validator, times(2)).validate(patToken);
        verify(userDetailsService, times(2)).loadUserByUsername("jdoe");
    }

    /**
     * Helper method to create a PersonalAccessToken.
     */
    private PersonalAccessToken createPersonalAccessToken(String publicId, String loginName) {
        var token = new PersonalAccessToken();
        token.setPublicId(publicId);
        token.setLoginName(loginName);
        token.setName("Test Token");
        token.setSecretHash("$2a$10$hashed_secret");
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(null);
        return token;
    }

    /**
     * Helper method to create UserDetails with authorities.
     */
    private UserDetails createUserDetails(String username, String... authorities) {
        List<GrantedAuthority> grantedAuthorities;
        if (authorities.length > 0) {
            grantedAuthorities = Stream.of(authorities)
                    .map(SimpleGrantedAuthority::new)
                    .map(GrantedAuthority.class::cast)
                    .toList();
        } else {
            grantedAuthorities = List.of();
        }

        return new User(username, "password", grantedAuthorities);
    }
}

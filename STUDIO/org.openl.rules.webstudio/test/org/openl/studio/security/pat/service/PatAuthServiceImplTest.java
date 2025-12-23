package org.openl.studio.security.pat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.openl.rules.security.standalone.persistence.PersonalAccessToken;
import org.openl.studio.security.pat.model.PatAuthResolution;
import org.openl.studio.security.pat.model.PatToken;
import org.openl.studio.security.pat.model.PatValidationResult;

/**
 * Unit tests for {@link PatAuthServiceImpl}.
 * Tests authentication resolution logic using mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
public class PatAuthServiceImplTest {

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
    public void setUp() {
        authService = new PatAuthServiceImpl(validator, userDetailsService);
    }

    @Test
    public void testResolveAuthentication_ValidToken() {
        // Arrange
        PatToken patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);
        PersonalAccessToken storedToken = createPersonalAccessToken(TEST_PUBLIC_ID, "jdoe");
        PatValidationResult validResult = PatValidationResult.valid(storedToken);

        UserDetails userDetails = createUserDetails("jdoe", "ROLE_USER", "ROLE_ADMIN");

        when(validator.validate(eq(patToken))).thenReturn(validResult);
        when(userDetailsService.loadUserByUsername(eq("jdoe"))).thenReturn(userDetails);

        // Act
        PatAuthResolution resolution = authService.resolveAuthentication(patToken);

        // Assert
        assertTrue(resolution.valid());
        assertNotNull(resolution.authentication());

        Authentication auth = resolution.authentication();
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, auth);
        assertSame(userDetails, auth.getPrincipal());
        assertNull(auth.getCredentials()); // Credentials should be null
        assertEquals(2, auth.getAuthorities().size());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        verify(validator, times(1)).validate(eq(patToken));
        verify(userDetailsService, times(1)).loadUserByUsername(eq("jdoe"));
    }

    @Test
    public void testResolveAuthentication_InvalidToken() {
        // Arrange
        PatToken patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);
        PatValidationResult invalidResult = PatValidationResult.invalid();

        when(validator.validate(eq(patToken))).thenReturn(invalidResult);

        // Act
        PatAuthResolution resolution = authService.resolveAuthentication(patToken);

        // Assert
        assertFalse(resolution.valid());
        assertNull(resolution.authentication());

        verify(validator, times(1)).validate(eq(patToken));
        verify(userDetailsService, never()).loadUserByUsername(eq("jdoe"));
    }


    @Test
    public void testResolveAuthentication_ValidToken_UserNotFound() {
        // Arrange
        PatToken patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);
        PersonalAccessToken storedToken = createPersonalAccessToken(TEST_PUBLIC_ID, "jdoe");
        PatValidationResult validResult = PatValidationResult.valid(storedToken);

        when(validator.validate(eq(patToken))).thenReturn(validResult);
        when(userDetailsService.loadUserByUsername(eq("jdoe")))
                .thenThrow(new UsernameNotFoundException("User not found"));

        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
                () -> authService.resolveAuthentication(patToken));

        verify(validator, times(1)).validate(eq(patToken));
        verify(userDetailsService, times(1)).loadUserByUsername(eq("jdoe"));
    }

    @Test
    public void testResolveAuthentication_UserWithoutAuthorities() {
        // Arrange
        PatToken patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);
        PersonalAccessToken storedToken = createPersonalAccessToken(TEST_PUBLIC_ID, "jdoe");
        PatValidationResult validResult = PatValidationResult.valid(storedToken);

        UserDetails userDetails = createUserDetails("jdoe"); // No authorities

        when(validator.validate(eq(patToken))).thenReturn(validResult);
        when(userDetailsService.loadUserByUsername(eq("jdoe"))).thenReturn(userDetails);

        // Act
        PatAuthResolution resolution = authService.resolveAuthentication(patToken);

        // Assert
        assertTrue(resolution.valid());
        assertNotNull(resolution.authentication());

        Authentication auth = resolution.authentication();
        assertTrue(auth.getAuthorities().isEmpty());

        verify(validator, times(1)).validate(eq(patToken));
        verify(userDetailsService, times(1)).loadUserByUsername(eq("jdoe"));
    }

    @Test
    public void testResolveAuthentication_UserWithSingleAuthority() {
        // Arrange
        PatToken patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);
        PersonalAccessToken storedToken = createPersonalAccessToken(TEST_PUBLIC_ID, "jdoe");
        PatValidationResult validResult = PatValidationResult.valid(storedToken);

        UserDetails userDetails = createUserDetails("jdoe", "ROLE_VIEWER");

        when(validator.validate(eq(patToken))).thenReturn(validResult);
        when(userDetailsService.loadUserByUsername(eq("jdoe"))).thenReturn(userDetails);

        // Act
        PatAuthResolution resolution = authService.resolveAuthentication(patToken);

        // Assert
        assertTrue(resolution.valid());
        assertNotNull(resolution.authentication());

        Authentication auth = resolution.authentication();
        assertEquals(1, auth.getAuthorities().size());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VIEWER")));

        verify(validator, times(1)).validate(eq(patToken));
        verify(userDetailsService, times(1)).loadUserByUsername(eq("jdoe"));
    }

    @Test
    public void testResolveAuthentication_MultipleTokensDifferentUsers() {
        // Arrange
        // First token for jdoe
        PatToken patToken1 = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);
        PersonalAccessToken storedToken1 = createPersonalAccessToken(TEST_PUBLIC_ID, "jdoe");
        PatValidationResult validResult1 = PatValidationResult.valid(storedToken1);
        UserDetails userDetails1 = createUserDetails("jdoe", "ROLE_USER");

        // Second token for jsmith
        PatToken patToken2 = new PatToken(TEST_PUBLIC_ID_2, TEST_SECRET_2);
        PersonalAccessToken storedToken2 = createPersonalAccessToken(TEST_PUBLIC_ID_2, "jsmith");
        PatValidationResult validResult2 = PatValidationResult.valid(storedToken2);
        UserDetails userDetails2 = createUserDetails("jsmith", "ROLE_ADMIN");

        when(validator.validate(eq(patToken1))).thenReturn(validResult1);
        when(validator.validate(eq(patToken2))).thenReturn(validResult2);
        when(userDetailsService.loadUserByUsername(eq("jdoe"))).thenReturn(userDetails1);
        when(userDetailsService.loadUserByUsername(eq("jsmith"))).thenReturn(userDetails2);

        // Act
        PatAuthResolution resolution1 = authService.resolveAuthentication(patToken1);
        PatAuthResolution resolution2 = authService.resolveAuthentication(patToken2);

        // Assert
        assertTrue(resolution1.valid());
        assertEquals("jdoe", ((UserDetails) resolution1.authentication().getPrincipal()).getUsername());
        assertTrue(resolution1.authentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));

        assertTrue(resolution2.valid());
        assertEquals("jsmith", ((UserDetails) resolution2.authentication().getPrincipal()).getUsername());
        assertTrue(resolution2.authentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        verify(validator, times(1)).validate(eq(patToken1));
        verify(validator, times(1)).validate(eq(patToken2));
        verify(userDetailsService, times(1)).loadUserByUsername(eq("jdoe"));
        verify(userDetailsService, times(1)).loadUserByUsername(eq("jsmith"));
    }

    @Test
    public void testResolveAuthentication_AuthenticationIsAuthenticated() {
        // Arrange
        PatToken patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);
        PersonalAccessToken storedToken = createPersonalAccessToken(TEST_PUBLIC_ID, "jdoe");
        PatValidationResult validResult = PatValidationResult.valid(storedToken);
        UserDetails userDetails = createUserDetails("jdoe", "ROLE_USER");

        when(validator.validate(eq(patToken))).thenReturn(validResult);
        when(userDetailsService.loadUserByUsername(eq("jdoe"))).thenReturn(userDetails);

        // Act
        PatAuthResolution resolution = authService.resolveAuthentication(patToken);

        // Assert
        assertTrue(resolution.valid());
        assertNotNull(resolution.authentication());

        // UsernamePasswordAuthenticationToken with principal and authorities is considered authenticated
        Authentication auth = resolution.authentication();
        assertTrue(auth.isAuthenticated(), "Authentication should be marked as authenticated");

        verify(validator, times(1)).validate(eq(patToken));
        verify(userDetailsService, times(1)).loadUserByUsername(eq("jdoe"));
    }

    @Test
    public void testResolveAuthentication_VerifyAuthenticationStructure() {
        // Arrange
        PatToken patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);
        PersonalAccessToken storedToken = createPersonalAccessToken(TEST_PUBLIC_ID, "jdoe");
        PatValidationResult validResult = PatValidationResult.valid(storedToken);
        UserDetails userDetails = createUserDetails("jdoe", "ROLE_USER", "ROLE_ADMIN");

        when(validator.validate(eq(patToken))).thenReturn(validResult);
        when(userDetailsService.loadUserByUsername(eq("jdoe"))).thenReturn(userDetails);

        // Act
        PatAuthResolution resolution = authService.resolveAuthentication(patToken);

        // Assert
        assertTrue(resolution.valid());
        Authentication auth = resolution.authentication();

        // Verify structure
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, auth);
        assertEquals(userDetails, auth.getPrincipal());
        assertNull(auth.getCredentials());

        // Verify authorities match (not strict collection equality)
        assertEquals(2, auth.getAuthorities().size());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        verify(validator, times(1)).validate(eq(patToken));
        verify(userDetailsService, times(1)).loadUserByUsername(eq("jdoe"));
    }

    @Test
    public void testResolveAuthentication_SameTokenMultipleTimes() {
        // Arrange
        PatToken patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);
        PersonalAccessToken storedToken = createPersonalAccessToken(TEST_PUBLIC_ID, "jdoe");
        PatValidationResult validResult = PatValidationResult.valid(storedToken);
        UserDetails userDetails = createUserDetails("jdoe", "ROLE_USER");

        when(validator.validate(eq(patToken))).thenReturn(validResult);
        when(userDetailsService.loadUserByUsername(eq("jdoe"))).thenReturn(userDetails);

        // Act - call multiple times with same token
        PatAuthResolution resolution1 = authService.resolveAuthentication(patToken);
        PatAuthResolution resolution2 = authService.resolveAuthentication(patToken);

        // Assert - both should succeed
        assertTrue(resolution1.valid());
        assertTrue(resolution2.valid());
        assertEquals("jdoe", ((UserDetails) resolution1.authentication().getPrincipal()).getUsername());
        assertEquals("jdoe", ((UserDetails) resolution2.authentication().getPrincipal()).getUsername());

        // Verify called twice
        verify(validator, times(2)).validate(eq(patToken));
        verify(userDetailsService, times(2)).loadUserByUsername(eq("jdoe"));
    }

    /**
     * Helper method to create a PersonalAccessToken.
     */
    private PersonalAccessToken createPersonalAccessToken(String publicId, String loginName) {
        PersonalAccessToken token = new PersonalAccessToken();
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
                    .map(a -> (GrantedAuthority) a)
                    .toList();
        } else {
            grantedAuthorities = List.of();
        }

        return new User(username, "password", grantedAuthorities);
    }
}

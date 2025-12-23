package org.openl.studio.security.pat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.openl.rules.security.standalone.persistence.PersonalAccessToken;
import org.openl.studio.security.pat.model.PatToken;
import org.openl.studio.users.model.pat.CreatedPersonalAccessTokenResponse;
import org.openl.studio.users.service.pat.PersonalAccessTokenService;

/**
 * Unit tests for {@link PatGeneratorServiceImpl}.
 * Tests token generation logic using mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
public class PatGeneratorServiceImplTest {

    private static final Instant FIXED_TIME = Instant.parse("2025-01-01T12:00:00Z");

    @Mock
    private PersonalAccessTokenService crudService;

    private PasswordEncoder passwordEncoder;
    private PatGeneratorService generatorService;

    @BeforeEach
    public void setUp() {
        // Use real BCrypt encoder for realistic password hashing
        passwordEncoder = new BCryptPasswordEncoder(10);

        // Use fixed clock for deterministic time-based testing
        Clock clock = Clock.fixed(FIXED_TIME, ZoneId.of("UTC"));

        // Create service with mocked CRUD service
        generatorService = new PatGeneratorServiceImpl(crudService, passwordEncoder, clock);
    }

    @Test
    public void testGenerateToken_WithoutExpiration() {
        // Arrange
        when(crudService.existsByPublicId(anyString())).thenReturn(false);

        // Act
        CreatedPersonalAccessTokenResponse response = generatorService.generateToken("jdoe", "My Token", null);

        // Assert
        assertNotNull(response);
        assertEquals("jdoe", response.loginName());
        assertEquals("My Token", response.name());
        assertEquals(FIXED_TIME, response.createdAt());
        assertNull(response.expiresAt());

        // Verify publicId format and length (16 chars, base62)
        assertNotNull(response.publicId());
        assertEquals(16, response.publicId().length());
        assertTrue(response.publicId().matches("^[0-9a-zA-Z]+$"));

        // Verify token format: openl_pat_<publicId>.<secret>
        assertNotNull(response.token());
        assertTrue(response.token().startsWith(PatToken.PREFIX));
        String[] parts = response.token().substring(PatToken.PREFIX.length()).split("\\.");
        assertEquals(2, parts.length, "Token should have publicId and secret parts");
        assertEquals(response.publicId(), parts[0], "Token publicId should match response publicId");
        assertEquals(32, parts[1].length(), "Secret should be 32 characters");

        // Verify service interactions
        verify(crudService, times(1)).existsByPublicId(anyString());
        verify(crudService, times(1)).save(any(PersonalAccessToken.class));
    }

    @Test
    public void testGenerateToken_WithFutureExpiration() {
        // Arrange
        Instant futureExpiration = FIXED_TIME.plusSeconds(86400); // 1 day later
        when(crudService.existsByPublicId(anyString())).thenReturn(false);

        // Act
        CreatedPersonalAccessTokenResponse response = generatorService.generateToken("jdoe", "My Token", futureExpiration);

        // Assert
        assertNotNull(response);
        assertEquals("jdoe", response.loginName());
        assertEquals("My Token", response.name());
        assertEquals(FIXED_TIME, response.createdAt());
        assertEquals(futureExpiration, response.expiresAt());

        verify(crudService, times(1)).save(any(PersonalAccessToken.class));
    }

    @Test
    public void testGenerateToken_WithPastExpiration_ThrowsException() {
        // Arrange
        Instant pastExpiration = FIXED_TIME.minusSeconds(3600); // 1 hour ago

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> generatorService.generateToken("jdoe", "My Token", pastExpiration));

        assertEquals("expiresAt must be in the future", exception.getMessage());

        // Verify no token was saved
        verify(crudService, never()).save(any(PersonalAccessToken.class));
    }

    @Test
    public void testGenerateToken_WithExpirationAtCurrentTime_IsValid() {
        // Arrange
        // Implementation uses isBefore(), so expiring exactly at current time is valid
        Instant currentTime = FIXED_TIME;
        when(crudService.existsByPublicId(anyString())).thenReturn(false);

        // Act
        CreatedPersonalAccessTokenResponse response = generatorService.generateToken("jdoe", "My Token", currentTime);

        // Assert
        assertNotNull(response);
        assertEquals(currentTime, response.expiresAt());

        verify(crudService, times(1)).save(any(PersonalAccessToken.class));
    }

    @Test
    public void testGenerateToken_HandlesPublicIdCollision() {
        // Arrange
        // Simulate collision on first attempt, then success
        when(crudService.existsByPublicId(anyString()))
                .thenReturn(true)   // First attempt: collision
                .thenReturn(true)   // Second attempt: collision
                .thenReturn(false); // Third attempt: success

        // Act
        CreatedPersonalAccessTokenResponse response = generatorService.generateToken("jdoe", "My Token", null);

        // Assert
        assertNotNull(response);
        assertEquals("jdoe", response.loginName());

        // Verify retry behavior - should check 3 times
        verify(crudService, times(3)).existsByPublicId(anyString());
        verify(crudService, times(1)).save(any(PersonalAccessToken.class));
    }

    @Test
    public void testGenerateToken_SavedTokenHasHashedSecret() {
        // Arrange
        when(crudService.existsByPublicId(anyString())).thenReturn(false);
        ArgumentCaptor<PersonalAccessToken> tokenCaptor = ArgumentCaptor.forClass(PersonalAccessToken.class);

        // Act
        CreatedPersonalAccessTokenResponse response = generatorService.generateToken("jdoe", "My Token", null);

        // Assert
        verify(crudService).save(tokenCaptor.capture());

        PersonalAccessToken savedToken = tokenCaptor.getValue();
        assertNotNull(savedToken.getSecretHash());

        // Verify secret is hashed (BCrypt hashes start with $2a$ or $2b$)
        assertTrue(savedToken.getSecretHash().startsWith("$2"), "Secret should be BCrypt hashed");

        // Verify raw secret from response is NOT the same as stored hash
        String rawSecret = response.token().substring(PatToken.PREFIX.length()).split("\\.")[1];
        assertNotEquals(rawSecret, savedToken.getSecretHash(), "Secret should be hashed before storage");

        // Verify BCrypt can validate the secret
        assertTrue(passwordEncoder.matches(rawSecret, savedToken.getSecretHash()),
                "Stored hash should match the raw secret");
    }

    @Test
    public void testGenerateToken_SavedTokenHasCorrectFields() {
        // Arrange
        Instant futureExpiration = FIXED_TIME.plusSeconds(86400);
        when(crudService.existsByPublicId(anyString())).thenReturn(false);
        ArgumentCaptor<PersonalAccessToken> tokenCaptor = ArgumentCaptor.forClass(PersonalAccessToken.class);

        // Act
        CreatedPersonalAccessTokenResponse response = generatorService.generateToken("jdoe", "My Token", futureExpiration);

        // Assert
        verify(crudService).save(tokenCaptor.capture());

        PersonalAccessToken savedToken = tokenCaptor.getValue();
        assertEquals(response.publicId(), savedToken.getPublicId());
        assertEquals("jdoe", savedToken.getLoginName());
        assertEquals("My Token", savedToken.getName());
        assertEquals(FIXED_TIME, savedToken.getCreatedAt());
        assertEquals(futureExpiration, savedToken.getExpiresAt());
        assertNotNull(savedToken.getSecretHash());
    }

    @Test
    public void testGenerateToken_MultipleCalls_GenerateDifferentTokens() {
        // Arrange
        when(crudService.existsByPublicId(anyString())).thenReturn(false);

        // Act
        CreatedPersonalAccessTokenResponse response1 = generatorService.generateToken("jdoe", "Token 1", null);
        CreatedPersonalAccessTokenResponse response2 = generatorService.generateToken("jdoe", "Token 2", null);

        // Assert
        assertNotEquals(response1.publicId(), response2.publicId(), "PublicIds should be different");
        assertNotEquals(response1.token(), response2.token(), "Full tokens should be different");

        verify(crudService, times(2)).save(any(PersonalAccessToken.class));
    }

    @Test
    public void testGenerateToken_ResponseContainsAllFields() {
        // Arrange
        Instant futureExpiration = FIXED_TIME.plusSeconds(86400);
        when(crudService.existsByPublicId(anyString())).thenReturn(false);

        // Act
        CreatedPersonalAccessTokenResponse response = generatorService.generateToken("jsmith", "Test Token", futureExpiration);

        // Assert - verify all fields are populated
        assertNotNull(response.publicId());
        assertNotNull(response.name());
        assertNotNull(response.loginName());
        assertNotNull(response.token());
        assertNotNull(response.createdAt());
        assertNotNull(response.expiresAt());

        assertEquals("jsmith", response.loginName());
        assertEquals("Test Token", response.name());
        assertEquals(FIXED_TIME, response.createdAt());
        assertEquals(futureExpiration, response.expiresAt());
    }

    @Test
    public void testGenerateToken_TokenCanBeParsed() {
        // Arrange
        when(crudService.existsByPublicId(anyString())).thenReturn(false);

        // Act
        CreatedPersonalAccessTokenResponse response = generatorService.generateToken("jdoe", "My Token", null);

        // Assert - verify token can be parsed back to PatToken
        PatToken parsedToken = PatToken.parse(response.token());
        assertEquals(response.publicId(), parsedToken.publicId());
        assertNotNull(parsedToken.secret());
        assertEquals(32, parsedToken.secret().length());
    }

    @Test
    public void testGenerateToken_VerifyPublicIdUniqueness() {
        // Arrange
        ArgumentCaptor<String> publicIdCaptor = ArgumentCaptor.forClass(String.class);
        when(crudService.existsByPublicId(anyString())).thenReturn(false);

        // Act
        generatorService.generateToken("jdoe", "My Token", null);

        // Assert
        verify(crudService).existsByPublicId(publicIdCaptor.capture());
        String checkedPublicId = publicIdCaptor.getValue();

        // Verify the publicId checked for uniqueness matches what was saved
        verify(crudService).save(argThat(token ->
                token.getPublicId().equals(checkedPublicId)
        ));
    }

    @Test
    public void testGenerateToken_DifferentUsers() {
        // Arrange
        when(crudService.existsByPublicId(anyString())).thenReturn(false);

        // Act
        CreatedPersonalAccessTokenResponse response1 = generatorService.generateToken("jdoe", "Token 1", null);
        CreatedPersonalAccessTokenResponse response2 = generatorService.generateToken("jsmith", "Token 2", null);

        // Assert
        assertEquals("jdoe", response1.loginName());
        assertEquals("jsmith", response2.loginName());
        assertNotEquals(response1.publicId(), response2.publicId());
        assertNotEquals(response1.token(), response2.token());

        verify(crudService, times(2)).save(any(PersonalAccessToken.class));
    }

    @Test
    public void testGenerateToken_WithLongTokenName() {
        // Arrange
        String longName = "A".repeat(100); // Max length per schema
        when(crudService.existsByPublicId(anyString())).thenReturn(false);

        // Act
        CreatedPersonalAccessTokenResponse response = generatorService.generateToken("jdoe", longName, null);

        // Assert
        assertEquals(longName, response.name());

        verify(crudService).save(argThat(token ->
                token.getName().equals(longName)
        ));
    }

    @Test
    public void testGenerateToken_PublicIdAndSecretAreBase62() {
        // Arrange
        when(crudService.existsByPublicId(anyString())).thenReturn(false);

        // Act
        CreatedPersonalAccessTokenResponse response = generatorService.generateToken("jdoe", "My Token", null);

        // Assert
        String fullToken = response.token();
        String tokenWithoutPrefix = fullToken.substring(PatToken.PREFIX.length());
        String[] parts = tokenWithoutPrefix.split("\\.");

        // Both publicId and secret should only contain base62 characters
        assertTrue(parts[0].matches("^[0-9a-zA-Z]+$"), "PublicId should be base62");
        assertTrue(parts[1].matches("^[0-9a-zA-Z]+$"), "Secret should be base62");

        // Verify they don't contain invalid characters
        assertFalse(parts[0].contains("-"), "PublicId should not contain hyphens");
        assertFalse(parts[0].contains("_"), "PublicId should not contain underscores");
        assertFalse(parts[1].contains("-"), "Secret should not contain hyphens");
        assertFalse(parts[1].contains("_"), "Secret should not contain underscores");
    }
}

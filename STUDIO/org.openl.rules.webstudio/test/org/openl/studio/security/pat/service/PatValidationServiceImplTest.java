package org.openl.studio.security.pat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.openl.rules.security.standalone.dao.PersonalAccessTokenDao;
import org.openl.rules.security.standalone.persistence.PersonalAccessToken;
import org.openl.studio.security.pat.model.PatToken;
import org.openl.studio.security.pat.model.PatValidationResult;

/**
 * Unit tests for {@link PatValidationServiceImpl}.
 * Tests token validation logic using mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
public class PatValidationServiceImplTest {

    // Valid test tokens matching PatToken format requirements (16 char publicId, 32 char secret)
    private static final String TEST_PUBLIC_ID = "abc123DEF4567890"; // 16 Base62 chars
    private static final String TEST_SECRET = "mySecret1234567890ABCDEF012345"; // 32 Base62 chars
    private static final String TEST_PUBLIC_ID_2 = "token1ABCDEF9012"; // 16 Base62 chars
    private static final String TEST_SECRET_2 = "secret1ABCDEF90123456789012345"; // 32 Base62 chars
    private static final String TEST_PUBLIC_ID_3 = "token2GHIJK34567"; // 16 Base62 chars
    private static final String TEST_SECRET_3 = "secret2GHIJK345678901234567890"; // 32 Base62 chars
    private static final String NONEXISTENT_ID = "nonexist12345678"; // 16 Base62 chars
    private static final String WRONG_SECRET = "wrongSecret12345678901234567890"; // 32 Base62 chars
    private static final Instant FIXED_TIME = Instant.parse("2025-01-01T12:00:00Z");

    @Mock
    private PersonalAccessTokenDao tokenDao;

    private PasswordEncoder passwordEncoder;
    private PatValidationService validationService;

    @BeforeEach
    public void setUp() {
        // Use real BCrypt encoder for realistic password hashing
        passwordEncoder = new BCryptPasswordEncoder(10);

        // Use fixed clock for deterministic time-based testing
        Clock clock = Clock.fixed(FIXED_TIME, ZoneId.of("UTC"));

        // Create service with mocked DAO
        validationService = new PatValidationServiceImpl(tokenDao, passwordEncoder, clock);
    }

    @Test
    public void testValidate_ValidToken() {
        // Arrange
        PersonalAccessToken storedToken = createToken(TEST_PUBLIC_ID, "jdoe", TEST_SECRET, null);
        when(tokenDao.getByPublicId(eq(TEST_PUBLIC_ID))).thenReturn(storedToken);

        PatToken patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);

        // Act
        PatValidationResult result = validationService.validate(patToken);

        // Assert
        assertTrue(result.valid());
        assertNotNull(result.token());
        assertSame(storedToken, result.token());
        assertEquals(TEST_PUBLIC_ID, result.token().getPublicId());
        assertEquals("jdoe", result.token().getLoginName());

        verify(tokenDao, times(1)).getByPublicId(eq(TEST_PUBLIC_ID));
    }

    @Test
    public void testValidate_TokenNotFound() {
        // Arrange
        when(tokenDao.getByPublicId(eq(NONEXISTENT_ID))).thenReturn(null);

        PatToken patToken = new PatToken(NONEXISTENT_ID, TEST_SECRET);

        // Act
        PatValidationResult result = validationService.validate(patToken);

        // Assert
        // Security Note: Returns INVALID without revealing the specific reason (token doesn't exist).
        // This prevents token enumeration attacks where attackers try to discover valid token IDs.
        assertFalse(result.valid());
        assertNull(result.token());

        verify(tokenDao, times(1)).getByPublicId(eq(NONEXISTENT_ID));
    }

    @Test
    public void testValidate_SecretMismatch() {
        // Arrange
        PersonalAccessToken storedToken = createToken(TEST_PUBLIC_ID, "jdoe", TEST_SECRET, null);
        when(tokenDao.getByPublicId(eq(TEST_PUBLIC_ID))).thenReturn(storedToken);

        // Try to validate with different secret
        PatToken patToken = new PatToken(TEST_PUBLIC_ID, WRONG_SECRET);

        // Act
        PatValidationResult result = validationService.validate(patToken);

        // Assert
        assertFalse(result.valid());
        assertNull(result.token());

        verify(tokenDao, times(1)).getByPublicId(eq(TEST_PUBLIC_ID));
    }

    @Test
    public void testValidate_ExpiredToken() {
        // Arrange
        Instant oneHourAgo = FIXED_TIME.minusSeconds(3600);
        PersonalAccessToken storedToken = createToken(TEST_PUBLIC_ID, "jdoe", TEST_SECRET, oneHourAgo);
        when(tokenDao.getByPublicId(eq(TEST_PUBLIC_ID))).thenReturn(storedToken);

        PatToken patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);

        // Act
        PatValidationResult result = validationService.validate(patToken);

        // Assert
        assertFalse(result.valid());
        assertNull(result.token());

        verify(tokenDao, times(1)).getByPublicId(eq(TEST_PUBLIC_ID));
    }

    @Test
    public void testValidate_TokenWithoutExpiration() {
        // Arrange
        PersonalAccessToken storedToken = createToken(TEST_PUBLIC_ID, "jdoe", TEST_SECRET, null);
        when(tokenDao.getByPublicId(eq(TEST_PUBLIC_ID))).thenReturn(storedToken);

        PatToken patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);

        // Act
        PatValidationResult result = validationService.validate(patToken);

        // Assert
        assertTrue(result.valid());
        assertNotNull(result.token());
        assertNull(result.token().getExpiresAt(), "Token should have no expiration");

        verify(tokenDao, times(1)).getByPublicId(eq(TEST_PUBLIC_ID));
    }

    @Test
    public void testValidate_TokenExpiresInFuture() {
        // Arrange
        Instant oneDayLater = FIXED_TIME.plusSeconds(86400);
        PersonalAccessToken storedToken = createToken(TEST_PUBLIC_ID, "jdoe", TEST_SECRET, oneDayLater);
        when(tokenDao.getByPublicId(eq(TEST_PUBLIC_ID))).thenReturn(storedToken);

        PatToken patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);

        // Act
        PatValidationResult result = validationService.validate(patToken);

        // Assert
        assertTrue(result.valid());
        assertNotNull(result.token());

        verify(tokenDao, times(1)).getByPublicId(eq(TEST_PUBLIC_ID));
    }

    @Test
    public void testValidate_TokenExpiresExactlyNow() {
        // Arrange
        // Token expires at exactly the current time (boundary condition)
        PersonalAccessToken storedToken = createToken(TEST_PUBLIC_ID, "jdoe", TEST_SECRET, FIXED_TIME);
        when(tokenDao.getByPublicId(eq(TEST_PUBLIC_ID))).thenReturn(storedToken);

        PatToken patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);

        // Act
        PatValidationResult result = validationService.validate(patToken);

        // Assert
        // Implementation uses isBefore(), so token expiring exactly at current time is still valid
        assertTrue(result.valid());
        assertNotNull(result.token());

        verify(tokenDao, times(1)).getByPublicId(eq(TEST_PUBLIC_ID));
    }

    @Test
    public void testValidate_TokenJustExpired() {
        // Arrange
        Instant justExpired = FIXED_TIME.minusMillis(1);
        PersonalAccessToken storedToken = createToken(TEST_PUBLIC_ID, "jdoe", TEST_SECRET, justExpired);
        when(tokenDao.getByPublicId(eq(TEST_PUBLIC_ID))).thenReturn(storedToken);

        PatToken patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);

        // Act
        PatValidationResult result = validationService.validate(patToken);

        // Assert
        assertFalse(result.valid());
        assertNull(result.token());

        verify(tokenDao, times(1)).getByPublicId(eq(TEST_PUBLIC_ID));
    }

    @Test
    public void testValidate_MultipleTokens() {
        // Arrange
        PersonalAccessToken token1 = createToken(TEST_PUBLIC_ID_2, "jdoe", TEST_SECRET_2, null);
        PersonalAccessToken token2 = createToken(TEST_PUBLIC_ID_3, "jsmith", TEST_SECRET_3, null);

        when(tokenDao.getByPublicId(eq(TEST_PUBLIC_ID_2))).thenReturn(token1);
        when(tokenDao.getByPublicId(eq(TEST_PUBLIC_ID_3))).thenReturn(token2);

        // Act & Assert - validate jdoe's token
        PatValidationResult result1 = validationService.validate(new PatToken(TEST_PUBLIC_ID_2, TEST_SECRET_2));
        assertTrue(result1.valid());
        assertEquals("jdoe", result1.token().getLoginName());

        // Act & Assert - validate jsmith's token
        PatValidationResult result2 = validationService.validate(new PatToken(TEST_PUBLIC_ID_3, TEST_SECRET_3));
        assertTrue(result2.valid());
        assertEquals("jsmith", result2.token().getLoginName());

        // Act & Assert - wrong secret for jdoe
        PatValidationResult result3 = validationService.validate(new PatToken(TEST_PUBLIC_ID_2, TEST_SECRET_3));
        assertFalse(result3.valid());

        verify(tokenDao, times(2)).getByPublicId(eq(TEST_PUBLIC_ID_2));
        verify(tokenDao, times(1)).getByPublicId(eq(TEST_PUBLIC_ID_3));
    }

    @Test
    public void testValidate_CaseSensitiveSecret() {
        // Arrange - Test that secrets are case-sensitive
        String caseSensitiveSecret = "MySecretABC123456789012345678XY"; // 32 chars
        String wrongCaseSecret = "mysecretabc123456789012345678xy"; // same but lowercase

        PersonalAccessToken storedToken = createToken(TEST_PUBLIC_ID, "jdoe", caseSensitiveSecret, null);
        when(tokenDao.getByPublicId(eq(TEST_PUBLIC_ID))).thenReturn(storedToken);

        // Act & Assert - exact match
        PatValidationResult result1 = validationService.validate(new PatToken(TEST_PUBLIC_ID, caseSensitiveSecret));
        assertTrue(result1.valid());

        // Act & Assert - wrong case
        PatValidationResult result2 = validationService.validate(new PatToken(TEST_PUBLIC_ID, wrongCaseSecret));
        assertFalse(result2.valid());

        verify(tokenDao, times(2)).getByPublicId(eq(TEST_PUBLIC_ID));
    }

    @Test
    public void testValidate_ExpiredTokenWithCorrectSecret() {
        // Arrange
        // Even though secret is correct, token should be rejected if expired
        Instant oneHourAgo = FIXED_TIME.minusSeconds(3600);
        PersonalAccessToken storedToken = createToken(TEST_PUBLIC_ID, "jdoe", TEST_SECRET, oneHourAgo);
        when(tokenDao.getByPublicId(eq(TEST_PUBLIC_ID))).thenReturn(storedToken);

        PatToken patToken = new PatToken(TEST_PUBLIC_ID, TEST_SECRET);

        // Act
        PatValidationResult result = validationService.validate(patToken);

        // Assert
        assertFalse(result.valid());
        assertNull(result.token(), "Expired token should not be returned even with correct secret");

        verify(tokenDao, times(1)).getByPublicId(eq(TEST_PUBLIC_ID));
    }

    /**
     * Helper method to create a PersonalAccessToken with encoded secret.
     */
    private PersonalAccessToken createToken(String publicId, String loginName, String secret, Instant expiresAt) {
        PersonalAccessToken token = new PersonalAccessToken();
        token.setPublicId(publicId);
        token.setSecretHash(passwordEncoder.encode(secret));
        token.setLoginName(loginName);
        token.setName("Token " + publicId);
        token.setCreatedAt(FIXED_TIME);
        token.setExpiresAt(expiresAt);
        return token;
    }
}

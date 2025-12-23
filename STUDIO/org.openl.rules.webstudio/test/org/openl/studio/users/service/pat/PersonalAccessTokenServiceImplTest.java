package org.openl.studio.users.service.pat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.security.standalone.persistence.PersonalAccessToken;
import org.openl.rules.webstudio.service.AclServiceTestConfiguration;
import org.openl.rules.webstudio.service.DBTestConfiguration;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.service.config.UserManagementConfiguration;
import org.openl.studio.users.model.pat.PersonalAccessTokenResponse;

/**
 * Unit tests for {@link PersonalAccessTokenServiceImpl}.
 * Tests CRUD operations with H2 in-memory database.
 */
@SpringJUnitConfig(classes = {DBTestConfiguration.class,
        UserManagementConfiguration.class,
        AclServiceTestConfiguration.class,
        PersonalAccessTokenTestConfiguration.class})
@TestPropertySource(properties = {"db.url = jdbc:h2:mem:temp;DB_CLOSE_DELAY=-1",
        "db.user =",
        "db.password =",
        "db.maximumPoolSize = 3"})
public class PersonalAccessTokenServiceImplTest {

    // Valid test publicIds matching validation requirements (exactly 16 Base62 chars)
    private static final String TEST_PUBLIC_ID_1 = "publicId12345678"; // 16 chars
    private static final String TEST_PUBLIC_ID_2 = "publicId2ABCDEFG"; // 16 chars
    private static final String TEST_PUBLIC_ID_3 = "publicId3HIJKLMN"; // 16 chars
    private static final String TEST_PUBLIC_ID_4 = "abc123DEF4567890"; // 16 chars

    @Autowired
    private PersonalAccessTokenService tokenService;

    @Autowired
    private UserManagementService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    @Qualifier("flywayDBReset")
    private Flyway flywayDBReset;

    @BeforeEach
    public void setUp() {
        // Reset all changes done while testing
        flywayDBReset.clean();
        flywayDBReset.migrate();
        QueryCountHolder.clear();
    }

    @Test
    public void testSaveAndGetTokensByUser() {
        initUser("jdoe");

        PersonalAccessToken token1 = createToken(TEST_PUBLIC_ID_1, "jdoe", "Token 1");
        PersonalAccessToken token2 = createToken(TEST_PUBLIC_ID_2, "jdoe", "Token 2");

        tokenService.save(token1);
        tokenService.save(token2);

        QueryCountHolder.clear();
        List<PersonalAccessTokenResponse> tokens = tokenService.getTokensByUser("jdoe");

        assertEquals(2, tokens.size());
        assertTrue(tokens.stream().anyMatch(t -> "Token 1".equals(t.name())));
        assertTrue(tokens.stream().anyMatch(t -> "Token 2".equals(t.name())));

        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getSelect());
        assertEquals(1, queryCount.getTotal());
    }

    @Test
    public void testGetTokenForUser() {
        initUser("jdoe");

        PersonalAccessToken token = createToken(TEST_PUBLIC_ID_4, "jdoe", "My Token");
        tokenService.save(token);

        QueryCountHolder.clear();
        PersonalAccessTokenResponse response = tokenService.getTokenForUser(TEST_PUBLIC_ID_4, "jdoe");

        assertNotNull(response);
        assertEquals(TEST_PUBLIC_ID_4, response.publicId());
        assertEquals("My Token", response.name());
        assertEquals("jdoe", response.loginName());

        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getSelect());
        assertEquals(1, queryCount.getTotal());
    }

    @Test
    public void testGetTokenForUser_WrongUser() {
        initUser("jdoe");
        initUser("jsmith");

        PersonalAccessToken token = createToken(TEST_PUBLIC_ID_4, "jdoe", "My Token");
        tokenService.save(token);

        QueryCountHolder.clear();
        PersonalAccessTokenResponse response = tokenService.getTokenForUser(TEST_PUBLIC_ID_4, "jsmith");

        assertNull(response, "Token should not be returned for different user");

        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getSelect());
        assertEquals(1, queryCount.getTotal());
    }

    @Test
    public void testGetTokenForUser_InvalidPublicId() {
        initUser("jdoe");

        // Test null
        assertNull(tokenService.getTokenForUser(null, "jdoe"));

        // Test wrong length (not exactly 16 chars)
        assertNull(tokenService.getTokenForUser("a".repeat(15), "jdoe")); // too short
        assertNull(tokenService.getTokenForUser("a".repeat(17), "jdoe")); // too long

        // Test invalid characters (not Base62)
        assertNull(tokenService.getTokenForUser("abc-123_12345678", "jdoe")); // 16 chars but with hyphen
        assertNull(tokenService.getTokenForUser("abc@123#12345678", "jdoe")); // 16 chars but with special chars
    }

    @Test
    public void testExistsByLoginNameAndName() {
        initUser("jdoe");

        PersonalAccessToken token = createToken(TEST_PUBLIC_ID_1, "jdoe", "My Token");
        tokenService.save(token);

        QueryCountHolder.clear();
        assertTrue(tokenService.existsByLoginNameAndName("jdoe", "My Token"));
        assertFalse(tokenService.existsByLoginNameAndName("jdoe", "Other Token"));
        assertFalse(tokenService.existsByLoginNameAndName("jsmith", "My Token"));

        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(3, queryCount.getSelect());
        assertEquals(3, queryCount.getTotal());
    }

    @Test
    public void testExistsByPublicId() {
        initUser("jdoe");

        PersonalAccessToken token = createToken(TEST_PUBLIC_ID_4, "jdoe", "My Token");
        tokenService.save(token);

        QueryCountHolder.clear();
        assertTrue(tokenService.existsByPublicId(TEST_PUBLIC_ID_4));
        assertFalse(tokenService.existsByPublicId("xyz789ABCDEF0123")); // 16 chars but doesn't exist

        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(2, queryCount.getSelect());
        assertEquals(2, queryCount.getTotal());
    }

    @Test
    public void testDeleteByPublicId() {
        initUser("jdoe");

        PersonalAccessToken token = createToken(TEST_PUBLIC_ID_4, "jdoe", "My Token");
        tokenService.save(token);

        assertTrue(tokenService.existsByPublicId(TEST_PUBLIC_ID_4));

        QueryCountHolder.clear();
        tokenService.deleteByPublicId(TEST_PUBLIC_ID_4);

        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getDelete());
        assertEquals(1, queryCount.getTotal());

        QueryCountHolder.clear();
        assertFalse(tokenService.existsByPublicId(TEST_PUBLIC_ID_4));
    }

    @Test
    public void testDeleteAllByUser() {
        initUser("jdoe");
        initUser("jsmith");

        PersonalAccessToken token1 = createToken(TEST_PUBLIC_ID_1, "jdoe", "Token 1");
        PersonalAccessToken token2 = createToken(TEST_PUBLIC_ID_2, "jdoe", "Token 2");
        PersonalAccessToken token3 = createToken(TEST_PUBLIC_ID_3, "jsmith", "Token 3");

        tokenService.save(token1);
        tokenService.save(token2);
        tokenService.save(token3);

        assertEquals(2, tokenService.getTokensByUser("jdoe").size());
        assertEquals(1, tokenService.getTokensByUser("jsmith").size());

        QueryCountHolder.clear();
        tokenService.deleteAllByUser("jdoe");

        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getDelete());
        assertEquals(1, queryCount.getTotal());

        QueryCountHolder.clear();
        assertTrue(tokenService.getTokensByUser("jdoe").isEmpty());
        assertEquals(1, tokenService.getTokensByUser("jsmith").size());
    }

    @Test
    public void testCascadeDeleteOnUserDeletion() {
        initUser("jdoe");

        PersonalAccessToken token1 = createToken(TEST_PUBLIC_ID_1, "jdoe", "Token 1");
        PersonalAccessToken token2 = createToken(TEST_PUBLIC_ID_2, "jdoe", "Token 2");

        tokenService.save(token1);
        tokenService.save(token2);

        assertEquals(2, tokenService.getTokensByUser("jdoe").size());

        // Delete user - should cascade delete tokens due to FK constraint
        userService.deleteUser("jdoe");

        QueryCountHolder.clear();
        assertTrue(tokenService.getTokensByUser("jdoe").isEmpty());

        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getSelect());
        assertEquals(1, queryCount.getTotal());
    }

    @Test
    public void testGetEmptyTokensList() {
        initUser("jdoe");

        QueryCountHolder.clear();
        List<PersonalAccessTokenResponse> tokens = tokenService.getTokensByUser("jdoe");

        assertTrue(tokens.isEmpty());

        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getSelect());
        assertEquals(1, queryCount.getTotal());
    }

    @Test
    public void testTokenResponseMapping() {
        initUser("jdoe");

        // Truncate to milliseconds to match database precision (H2 doesn't preserve nanoseconds)
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Instant expires = now.plusSeconds(86400); // 1 day

        PersonalAccessToken token = new PersonalAccessToken();
        token.setPublicId(TEST_PUBLIC_ID_4);
        token.setSecretHash(passwordEncoder.encode("secret"));
        token.setLoginName("jdoe");
        token.setName("Test Token");
        token.setCreatedAt(now);
        token.setExpiresAt(expires);

        tokenService.save(token);

        QueryCountHolder.clear();
        PersonalAccessTokenResponse response = tokenService.getTokenForUser(TEST_PUBLIC_ID_4, "jdoe");

        assertNotNull(response);
        assertEquals(TEST_PUBLIC_ID_4, response.publicId());
        assertEquals("Test Token", response.name());
        assertEquals("jdoe", response.loginName());
        assertEquals(now, response.createdAt());
        assertEquals(expires, response.expiresAt());
    }

    @Test
    public void testTokenWithoutExpiration() {
        initUser("jdoe");

        PersonalAccessToken token = new PersonalAccessToken();
        token.setPublicId(TEST_PUBLIC_ID_4);
        token.setSecretHash(passwordEncoder.encode("secret"));
        token.setLoginName("jdoe");
        token.setName("No Expiry Token");
        token.setCreatedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        token.setExpiresAt(null); // No expiration

        tokenService.save(token);

        QueryCountHolder.clear();
        PersonalAccessTokenResponse response = tokenService.getTokenForUser(TEST_PUBLIC_ID_4, "jdoe");

        assertNotNull(response);
        assertNull(response.expiresAt(), "Token should have no expiration");
    }

    @Test
    public void testMultipleUsersWithSameTokenName() {
        initUser("jdoe");
        initUser("jsmith");

        // Both users can have tokens with the same name (different users)
        PersonalAccessToken token1 = createToken(TEST_PUBLIC_ID_1, "jdoe", "My Token");
        PersonalAccessToken token2 = createToken(TEST_PUBLIC_ID_2, "jsmith", "My Token");

        tokenService.save(token1);
        tokenService.save(token2);

        QueryCountHolder.clear();
        assertTrue(tokenService.existsByLoginNameAndName("jdoe", "My Token"));
        assertTrue(tokenService.existsByLoginNameAndName("jsmith", "My Token"));

        List<PersonalAccessTokenResponse> jdoeTokens = tokenService.getTokensByUser("jdoe");
        List<PersonalAccessTokenResponse> jsmithTokens = tokenService.getTokensByUser("jsmith");

        assertEquals(1, jdoeTokens.size());
        assertEquals(1, jsmithTokens.size());
        assertEquals(TEST_PUBLIC_ID_1, jdoeTokens.getFirst().publicId());
        assertEquals(TEST_PUBLIC_ID_2, jsmithTokens.getFirst().publicId());
    }

    private PersonalAccessToken createToken(String publicId, String loginName, String name) {
        PersonalAccessToken token = new PersonalAccessToken();
        token.setPublicId(publicId);
        token.setSecretHash(passwordEncoder.encode("secret"));
        token.setLoginName(loginName);
        token.setName(name);
        token.setCreatedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        return token;
    }

    private void initUser(String loginName) {
        userService.addUser(loginName,
                "First",
                "Last",
                "password",
                loginName + "@test.com",
                loginName.toUpperCase());
        QueryCountHolder.clear();
    }
}

package org.openl.rules.webstudio.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.flywaydb.core.Flyway;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.security.User;
import org.openl.rules.security.UserExternalFlags;
import org.openl.rules.security.UserExternalFlags.Feature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.QueryCountHolder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DBTestConfiguration.class)
@TestPropertySource(properties = { "db.url = jdbc:h2:mem:temp;DB_CLOSE_DELAY=-1", "db.user =", "db.password =" })
public class UserManagementTest {

    @Autowired
    private UserManagementService userService;

    @Autowired
    @Qualifier("flywayDBReset")
    private Flyway flywayDBReset;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Before
    public void setUp() {
        // Reset all changes where done while testing
        flywayDBReset.clean();
        flywayDBReset.migrate();
        QueryCountHolder.clear();
    }

    @Test
    public void testInitialization() {
        userService
            .addUser("jdoe", "John", "Doe", "qwerty", "jdoe@test", "John Doe");
        assertTrue(userService.existsByName("jdoe"));
        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getSelect());
        assertEquals(1, queryCount.getInsert());
        assertEquals(2, queryCount.getTotal());
    }

    @Test
    public void testSaveUser() {
        userService.addUser("jdoe", "John", "Doe", "qwerty", "jdoe@test", "John Doe");
        User user = userService.getUser("jdoe");
        assertNotNull(user);
        assertEquals("jdoe", user.getUsername());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertTrue(passwordEncoder.matches("qwerty", user.getPassword()));
        assertEquals("jdoe@test", user.getEmail());
        assertEquals("John Doe", user.getDisplayName());
        assertTrue(user.getAuthorities().isEmpty());
        assertFalse(user.getExternalFlags().isEmailExternal());
        assertFalse(user.getExternalFlags().isDisplayNameExternal());
        assertFalse(user.getExternalFlags().isFirstNameExternal());
        assertFalse(user.getExternalFlags().isLastNameExternal());
        assertFalse(user.getExternalFlags().isEmailVerified());
        final int expectedFeatures = UserExternalFlags.builder().getRawFeatures();
        assertEquals(expectedFeatures, UserExternalFlags.builder(user.getExternalFlags()).getRawFeatures());
        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(2, queryCount.getSelect());
        assertEquals(1, queryCount.getInsert());
        assertEquals(3, queryCount.getTotal());
    }

    @Test
    public void testSyncNewUser() {
        userService.syncUserData("jdoe", "John", "Doe", "jdoe@test", "John Doe");
        User user = userService.getUser("jdoe");
        assertNotNull(user);
        assertEquals("jdoe", user.getUsername());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertNull(user.getPassword());
        assertEquals("jdoe@test", user.getEmail());
        assertEquals("John Doe", user.getDisplayName());
        assertTrue(user.getAuthorities().isEmpty());
        assertTrue(user.getExternalFlags().isEmailExternal());
        assertTrue(user.getExternalFlags().isDisplayNameExternal());
        assertTrue(user.getExternalFlags().isFirstNameExternal());
        assertTrue(user.getExternalFlags().isLastNameExternal());
        assertTrue(user.getExternalFlags().isEmailVerified());
        final int expectedFeatures = UserExternalFlags.builder()
                .withFeature(Feature.EXTERNAL_DISPLAY_NAME)
                .withFeature(Feature.EXTERNAL_LAST_NAME)
                .withFeature(Feature.EXTERNAL_FIRST_NAME)
                .withFeature(Feature.EXTERNAL_EMAIL)
                .withFeature(Feature.EMAIL_VERIFIED)
                .getRawFeatures();
        assertEquals(expectedFeatures, UserExternalFlags.builder(user.getExternalFlags()).getRawFeatures());
        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(4, queryCount.getSelect());
        assertEquals(1, queryCount.getInsert());
        assertEquals(5, queryCount.getTotal());
    }

    @Test
    public void testSyncUser() {
        userService.addUser("jdoe", "John", "Doe", "qwerty", "jdoe@test", "John Doe");
        userService.syncUserData("jdoe", "John2", "Doe3", "jdoe@test4", "John Doe5");
        User user = userService.getUser("jdoe");
        assertNotNull(user);
        assertEquals("jdoe", user.getUsername());
        assertEquals("John2", user.getFirstName());
        assertEquals("Doe3", user.getLastName());
        assertNull(user.getPassword());
        assertEquals("jdoe@test4", user.getEmail());
        assertEquals("John Doe5", user.getDisplayName());
        assertTrue(user.getAuthorities().isEmpty());
        assertTrue(user.getExternalFlags().isEmailExternal());
        assertTrue(user.getExternalFlags().isDisplayNameExternal());
        assertTrue(user.getExternalFlags().isFirstNameExternal());
        assertTrue(user.getExternalFlags().isLastNameExternal());
        assertTrue(user.getExternalFlags().isEmailVerified());
        final int expectedFeatures = UserExternalFlags.builder()
            .withFeature(Feature.EXTERNAL_DISPLAY_NAME)
            .withFeature(Feature.EXTERNAL_LAST_NAME)
            .withFeature(Feature.EXTERNAL_FIRST_NAME)
            .withFeature(Feature.EXTERNAL_EMAIL)
            .withFeature(Feature.EMAIL_VERIFIED)
            .getRawFeatures();
        assertEquals(expectedFeatures, UserExternalFlags.builder(user.getExternalFlags()).getRawFeatures());
        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(5, queryCount.getSelect());
        assertEquals(1, queryCount.getInsert());
        assertEquals(1, queryCount.getUpdate());
        assertEquals(7, queryCount.getTotal());
    }

    @Test
    public void testReSyncUser() {
        userService.syncUserData("jdoe", "John", "Doe", "jdoe@test", "Doe John");
        userService.syncUserData("jdoe", "John2", "Doe3", null, null);
        User user = userService.getUser("jdoe");
        assertNotNull(user);
        assertEquals("jdoe", user.getUsername());
        assertEquals("John2", user.getFirstName());
        assertEquals("Doe3", user.getLastName());
        assertNull(user.getPassword());
        assertEquals("jdoe@test", user.getEmail());
        assertEquals("Doe3 John2", user.getDisplayName());
        assertTrue(user.getAuthorities().isEmpty());
        assertFalse(user.getExternalFlags().isEmailExternal());
        assertFalse(user.getExternalFlags().isDisplayNameExternal());
        assertTrue(user.getExternalFlags().isFirstNameExternal());
        assertTrue(user.getExternalFlags().isLastNameExternal());
        assertTrue(user.getExternalFlags().isEmailVerified());
        final int expectedFeatures = UserExternalFlags.builder()
                .withoutFeature(Feature.EXTERNAL_DISPLAY_NAME)
                .withFeature(Feature.EXTERNAL_LAST_NAME)
                .withFeature(Feature.EXTERNAL_FIRST_NAME)
                .withoutFeature(Feature.EXTERNAL_EMAIL)
                .withFeature(Feature.EMAIL_VERIFIED)
                .getRawFeatures();
        assertEquals(expectedFeatures, UserExternalFlags.builder(user.getExternalFlags()).getRawFeatures());
        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(7, queryCount.getSelect());
        assertEquals(1, queryCount.getInsert());
        assertEquals(1, queryCount.getUpdate());
        assertEquals(9, queryCount.getTotal());
    }

    @Test
    public void testReSyncNullUser() {
        userService.syncUserData("jdoe", "John", "Doe", "jdoe@test", "Jon Doe");
        userService.syncUserData("jdoe", null, null, null, null);
        User user = userService.getUser("jdoe");
        assertNotNull(user);
        assertEquals("jdoe", user.getUsername());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertNull(user.getPassword());
        assertEquals("jdoe@test", user.getEmail());
        assertEquals("Jon Doe", user.getDisplayName());
        assertFalse(user.getExternalFlags().isEmailExternal());
        assertFalse(user.getExternalFlags().isDisplayNameExternal());
        assertFalse(user.getExternalFlags().isFirstNameExternal());
        assertFalse(user.getExternalFlags().isLastNameExternal());
        assertTrue(user.getExternalFlags().isEmailVerified());
        final int expectedFeatures = UserExternalFlags.builder()
                .withoutFeature(Feature.EXTERNAL_DISPLAY_NAME)
                .withoutFeature(Feature.EXTERNAL_LAST_NAME)
                .withoutFeature(Feature.EXTERNAL_FIRST_NAME)
                .withoutFeature(Feature.EXTERNAL_EMAIL)
                .withFeature(Feature.EMAIL_VERIFIED)
                .getRawFeatures();
        assertEquals(expectedFeatures, UserExternalFlags.builder(user.getExternalFlags()).getRawFeatures());
        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(7, queryCount.getSelect());
        assertEquals(1, queryCount.getInsert());
        assertEquals(1, queryCount.getUpdate());
        assertEquals(9, queryCount.getTotal());
    }

    @Test
    public void testEmpty() {
        assertFalse(userService.existsByName("Foo"));

        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getSelect());
        assertEquals(1, queryCount.getTotal());
    }

}

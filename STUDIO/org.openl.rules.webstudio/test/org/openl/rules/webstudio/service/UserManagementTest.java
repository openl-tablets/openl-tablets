package org.openl.rules.webstudio.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
            .addUser("jdoe", "John", "Doe", "qwerty", "jdoe@test", "John Doe", UserExternalFlags.builder().build());
        assertTrue(userService.existsByName("jdoe"));
        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getSelect());
        assertEquals(1, queryCount.getInsert());
        assertEquals(2, queryCount.getTotal());
    }

    @Test
    public void testSaveUser() {
        final UserExternalFlags actualFeatures = UserExternalFlags.builder().build();
        userService.addUser("jdoe", "John", "Doe", "qwerty", "jdoe@test", "John Doe", actualFeatures);
        User user = userService.getUser("jdoe");
        assertNotNull(user);
        assertEquals("jdoe", user.getUsername());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("qwerty", user.getPassword());
        assertEquals("jdoe@test", user.getEmail());
        assertEquals("John Doe", user.getDisplayName());
        assertTrue(user.getAuthorities().isEmpty());
        assertFalse(user.getExternalFlags().isEmailExternal());
        assertFalse(user.getExternalFlags().isDisplayNameExternal());
        assertFalse(user.getExternalFlags().isFirstNameExternal());
        assertFalse(user.getExternalFlags().isLastNameExternal());
        assertFalse(user.getExternalFlags().isSyncExternalGroups()); // Always must be false
        assertFalse(user.getExternalFlags().isEmailVerified());
        final int expectedFeatures = UserExternalFlags.builder().getRawFeatures();
        assertEquals(expectedFeatures, UserExternalFlags.builder(user.getExternalFlags()).getRawFeatures());
        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(2, queryCount.getSelect());
        assertEquals(1, queryCount.getInsert());
        assertEquals(3, queryCount.getTotal());
    }

    @Test
    public void testSaveUser2() {
        final UserExternalFlags actualFeatures = UserExternalFlags.builder()
            .withFeature(Feature.EXTERNAL_DISPLAY_NAME)
            .withFeature(Feature.SYNC_EXTERNAL_GROUPS)
            .withFeature(Feature.EXTERNAL_LAST_NAME)
            .withFeature(Feature.EXTERNAL_FIRST_NAME)
            .withFeature(Feature.EXTERNAL_EMAIL)
            .withFeature(Feature.EMAIL_VERIFIED)
            .build();
        userService.addUser("jdoe", "John", "Doe", "qwerty", "jdoe@test", "John Doe", actualFeatures);
        User user = userService.getUser("jdoe");
        assertNotNull(user);
        assertEquals("jdoe", user.getUsername());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("qwerty", user.getPassword());
        assertEquals("jdoe@test", user.getEmail());
        assertEquals("John Doe", user.getDisplayName());
        assertTrue(user.getAuthorities().isEmpty());
        assertTrue(user.getExternalFlags().isEmailExternal());
        assertTrue(user.getExternalFlags().isDisplayNameExternal());
        assertTrue(user.getExternalFlags().isFirstNameExternal());
        assertTrue(user.getExternalFlags().isLastNameExternal());
        assertFalse(user.getExternalFlags().isSyncExternalGroups()); // Always must be false
        assertTrue(user.getExternalFlags().isEmailVerified());
        final int expectedFeatures = UserExternalFlags.builder()
            .withFeature(Feature.EXTERNAL_DISPLAY_NAME)
            .withoutFeature(Feature.SYNC_EXTERNAL_GROUPS) // Always must be false
            .withFeature(Feature.EXTERNAL_LAST_NAME)
            .withFeature(Feature.EXTERNAL_FIRST_NAME)
            .withFeature(Feature.EXTERNAL_EMAIL)
            .withFeature(Feature.EMAIL_VERIFIED)
            .getRawFeatures();
        assertEquals(expectedFeatures, UserExternalFlags.builder(user.getExternalFlags()).getRawFeatures());
        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(2, queryCount.getSelect());
        assertEquals(1, queryCount.getInsert());
        assertEquals(3, queryCount.getTotal());
    }

    @Test
    public void testEmpty() {
        assertFalse(userService.existsByName("Foo"));

        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getSelect());
        assertEquals(1, queryCount.getTotal());
    }

}

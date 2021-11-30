package org.openl.rules.webstudio.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.flywaydb.core.Flyway;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.security.UserExternalFlags;
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
    public void testEmpty() {
        assertFalse(userService.existsByName("Foo"));

        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getSelect());
        assertEquals(1, queryCount.getTotal());
    }

}

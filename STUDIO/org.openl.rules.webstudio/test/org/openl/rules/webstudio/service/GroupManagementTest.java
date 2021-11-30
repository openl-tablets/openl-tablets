package org.openl.rules.webstudio.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flywaydb.core.Flyway;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.security.Group;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimplePrivilege;
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
public class GroupManagementTest {

    private static final String RND_GROUP = "GROUP_%s";

    @Autowired
    private ExternalGroupService externalGroupService;
    @Autowired
    private UserManagementService userService;
    @Autowired
    private GroupManagementService groupService;

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
        initOneUser();
        Set<Privilege> privileges = generatePrivilege(51, "Analysts", "Deployers");
        externalGroupService.mergeAllForUser("jdoe", privileges);
        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(2, queryCount.getInsert());
        assertEquals(1, queryCount.getDelete());
        assertEquals(3, queryCount.getTotal());

        QueryCountHolder.clear();
        List<Group> extGroups = externalGroupService.findAllForUser("jdoe");
        assertCollectionEquals(privileges.stream().map(Privilege::getName).collect(Collectors.toList()),
            extGroups,
            Group::getName);
        queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getSelect());
        assertEquals(1, queryCount.getTotal());

        QueryCountHolder.clear();
        long cntExternalGroups = externalGroupService.countAllForUser("jdoe");
        assertEquals(privileges.size(), cntExternalGroups);
        queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getSelect());
        assertEquals(1, queryCount.getTotal());

        QueryCountHolder.clear();
        List<Group> matchedExtGroups = externalGroupService.findMatchedForUser("jdoe");
        assertCollectionEquals(Stream.of("Analysts", "Deployers").collect(Collectors.toSet()),
            matchedExtGroups,
            Group::getName);
        queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(11, queryCount.getSelect());
        assertEquals(11, queryCount.getTotal());

        QueryCountHolder.clear();
        long cntMatchedExternalGroups = externalGroupService.countMatchedForUser("jdoe");
        assertEquals(2, cntMatchedExternalGroups);
        queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getSelect());
        assertEquals(1, queryCount.getTotal());

        QueryCountHolder.clear();
        List<Group> notMatchedExtGroups = externalGroupService.findNotMatchedForUser("jdoe");
        assertCollectionEquals(privileges.stream()
            .map(Privilege::getName)
            .filter(p -> !"Analysts".equals(p) && !"Deployers".equals(p))
            .collect(Collectors.toList()), notMatchedExtGroups, Group::getName);
        queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getSelect());
        assertEquals(1, queryCount.getTotal());

        QueryCountHolder.clear();
        long cntNotMatchedExternalGroups = externalGroupService.countNotMatchedForUser("jdoe");
        assertEquals(51, cntNotMatchedExternalGroups);
        queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getSelect());
        assertEquals(1, queryCount.getTotal());

        QueryCountHolder.clear();
        userService.deleteUser("jdoe");
        queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getDelete());
        assertEquals(1, queryCount.getTotal());

        QueryCountHolder.clear();
        extGroups = externalGroupService.findAllForUser("jdoe");
        assertTrue(extGroups.isEmpty());

        queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getSelect());
        assertEquals(1, queryCount.getTotal());
    }

    @Test
    public void testDeleteAll() {
        initOneUser();

        Set<Privilege> privileges = generatePrivilege(10, "Analysts", "Deployers");
        externalGroupService.mergeAllForUser("jdoe", privileges);
        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getInsert());
        assertEquals(1, queryCount.getDelete());
        assertEquals(2, queryCount.getTotal());

        QueryCountHolder.clear();
        List<Group> extGroups = externalGroupService.findAllForUser("jdoe");
        assertCollectionEquals(privileges.stream().map(Privilege::getName).collect(Collectors.toList()),
            extGroups,
            Group::getName);
        long cntExternalGroups = externalGroupService.countAllForUser("jdoe");
        assertEquals(privileges.size(), cntExternalGroups);
        queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(2, queryCount.getSelect());
        assertEquals(2, queryCount.getTotal());

        QueryCountHolder.clear();
        externalGroupService.deleteAllForUser("jdoe");
        queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getDelete());
        assertEquals(1, queryCount.getTotal());

        QueryCountHolder.clear();
        testEmpty();
    }

    @Test
    public void testDefaultGroups() {
        List<Group> groups = groupService.getGroups();
        assertEquals(6, groups.size());
        assertCollectionEquals(Stream.of("Administrators", "Analysts", "Deployers", "Developers", "Testers", "Viewers")
            .collect(Collectors.toList()), groups, Group::getName);

        Map<String, Group> mappedGroups = groups.stream()
            .collect(Collectors.toMap(Group::getName, Function.identity()));

        assertCollectionEquals(Stream.of("ADMIN").collect(Collectors.toList()),
            mappedGroups.get("Administrators").getPrivileges(),
            Privilege::getName);

        assertCollectionEquals(Stream.of("Developers", "Testers").collect(Collectors.toList()),
            mappedGroups.get("Analysts").getPrivileges(),
            Privilege::getName);

        assertCollectionEquals(
            Stream
                .of("Viewers",
                    "DELETE_DEPLOYMENT",
                    "ERASE_DEPLOYMENT",
                    "CREATE_DEPLOYMENT",
                    "DEPLOY_PROJECTS",
                    "EDIT_DEPLOYMENT")
                .collect(Collectors.toList()),
            mappedGroups.get("Deployers").getPrivileges(),
            Privilege::getName);

        assertCollectionEquals(
            Stream
                .of("Viewers",
                    "CREATE_PROJECTS",
                    "CREATE_TABLES",
                    "ERASE_PROJECTS",
                    "REMOVE_TABLES",
                    "EDIT_PROJECTS",
                    "EDIT_TABLES",
                    "DELETE_PROJECTS")
                .collect(Collectors.toList()),
            mappedGroups.get("Developers").getPrivileges(),
            Privilege::getName);

        assertCollectionEquals(Stream.of("TRACE", "BENCHMARK", "RUN", "Viewers").collect(Collectors.toList()),
            mappedGroups.get("Testers").getPrivileges(),
            Privilege::getName);

        assertCollectionEquals(Stream.of("VIEW_PROJECTS").collect(Collectors.toList()),
            mappedGroups.get("Viewers").getPrivileges(),
            Privilege::getName);

        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(13, queryCount.getSelect());
        assertEquals(13, queryCount.getTotal());
    }

    @Test
    public void testEmpty() {
        assertFalse(userService.existsByName("Foo"));

        assertTrue(externalGroupService.findAllForUser("Foo").isEmpty());
        assertTrue(externalGroupService.findMatchedForUser("Foo").isEmpty());
        assertTrue(externalGroupService.findNotMatchedForUser("Foo").isEmpty());

        assertEquals(0, externalGroupService.countAllForUser("Foo"));
        assertEquals(0, externalGroupService.countMatchedForUser("Foo"));
        assertEquals(0, externalGroupService.countNotMatchedForUser("Foo"));

        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(7, queryCount.getSelect());
        assertEquals(7, queryCount.getTotal());
    }

    private static <T, R> void assertCollectionEquals(Collection<R> expected,
            Collection<T> actual,
            Function<T, R> attr) {
        List<R> transformedActual = actual.stream().map(attr).collect(Collectors.toList());
        List<R> rest = new ArrayList<>(transformedActual);
        rest.removeAll(expected);
        if (!rest.isEmpty()) {
            fail(String.format("Unexpected items: %s",
                rest.stream().map(Object::toString).collect(Collectors.joining(" ,"))));
        }

        rest = new ArrayList<>(expected);
        rest.removeAll(transformedActual);
        if (!rest.isEmpty()) {
            fail(String.format("Missed expected items: %s",
                rest.stream().map(Object::toString).collect(Collectors.joining(" ,"))));
        }
    }

    private Set<Privilege> generatePrivilege(int count, String... defaultGroups) {
        Set<Privilege> res = new HashSet<>();
        Stream.of(defaultGroups).map(name -> new SimplePrivilege(name, name)).forEach(res::add);
        for (int i = 0; i < count; i++) {
            String name = String.format(RND_GROUP, i);
            res.add(new SimplePrivilege(name, name));
        }
        return Collections.unmodifiableSet(res);
    }

    private void initOneUser() {
        userService
            .addUser("jdoe", "John", "Doe", "qwerty", "jdoe@test", "John Doe", UserExternalFlags.builder().build());
        QueryCountHolder.clear();
    }

}

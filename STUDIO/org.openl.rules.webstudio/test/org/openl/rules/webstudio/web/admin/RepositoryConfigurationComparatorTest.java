package org.openl.rules.webstudio.web.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import org.openl.rules.webstudio.web.admin.RepositoryConfiguration.NameWithNumbersComparator;

public class RepositoryConfigurationComparatorTest {

    @Test
    public void testCompare() {
        NameWithNumbersComparator comparator = new NameWithNumbersComparator();

        assertTrue(comparator.compare(configMock("aaa"), configMock("bbb")) < 0);
        assertTrue(comparator.compare(configMock("bbb"), configMock("aaa")) > 0);

        assertTrue(comparator.compare(configMock("Production"), configMock("Production1")) < 0);
        assertTrue(comparator.compare(configMock("Production1"), configMock("Production")) > 0);

        assertTrue(comparator.compare(configMock("Production1"), configMock("Production2")) < 0);
        assertTrue(comparator.compare(configMock("Production2"), configMock("Production10")) < 0);
        assertTrue(comparator.compare(configMock("Production2test5"), configMock("Production2test50")) < 0);

        assertTrue(comparator.compare(configMock("Production2"), configMock("Production1")) > 0);
        assertTrue(comparator.compare(configMock("Production10"), configMock("Production2")) > 0);
        assertTrue(comparator.compare(configMock("Production2test50"), configMock("Production2test5")) > 0);

        assertTrue(comparator.compare(configMock("mix-34.in5test"), configMock("mix-34.in6test")) < 0);

        assertEquals(0, comparator.compare(configMock("Production"), configMock("Production")));
        assertEquals(0, comparator.compare(configMock("Production134"), configMock("production134")));
        assertEquals(0, comparator.compare(configMock("345"), configMock("345")));
        assertEquals(0, comparator.compare(configMock(""), configMock("")));
    }

    private RepositoryConfiguration configMock(String name) {
        RepositoryConfiguration config = mock(RepositoryConfiguration.class);
        when(config.getName()).thenReturn(name);
        return config;
    }

}

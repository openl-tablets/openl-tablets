package org.openl.rules.workspace.dtr.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.openl.rules.dataformat.yaml.YamlMapperFactory;

public class ProjectIndexSerializerTest {

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();

    @BeforeClass
    public static void setUp() {
        Locale.setDefault(Locale.US);
        // set +2 as default
        TimeZone defaultTimeZone = TimeZone.getTimeZone("Europe/Helsinki");
        TimeZone.setDefault(defaultTimeZone);
    }

    @AfterClass
    public static void tearDown() {
        Locale.setDefault(DEFAULT_LOCALE);
        TimeZone.setDefault(DEFAULT_TIMEZONE);
    }

    private final YAMLMapper mapper = YamlMapperFactory.getYamlMapper();

    @Test
    public void test() throws IOException {
        ProjectIndex projectIndex = null;
        try (InputStream stream = getClass().getResourceAsStream("/openl-projects.yaml")) {
            projectIndex = mapper.readValue(stream, ProjectIndex.class);
            assertTheSame(projectIndex);
        }
        assertNotNull(projectIndex);
        assertTheSame(mapper.readValue(mapper.writeValueAsBytes(projectIndex), ProjectIndex.class));
    }

    private static void assertTheSame(ProjectIndex projectIndex) {
        assertEquals(3, projectIndex.getProjects().size());

        assertEquals("Project", projectIndex.getProjects().get(0).getName());
        assertEquals("Project", projectIndex.getProjects().get(0).getPath());
        assertNull(projectIndex.getProjects().get(0).getModifiedAt());
        assertFalse(projectIndex.getProjects().get(0).isArchived());

        assertEquals("Project4", projectIndex.getProjects().get(1).getName());
        assertEquals("foo/bar/Project4", projectIndex.getProjects().get(1).getPath());
        assertNull(projectIndex.getProjects().get(1).getModifiedAt());
        assertTrue(projectIndex.getProjects().get(1).isArchived());

        assertEquals("Project7", projectIndex.getProjects().get(2).getName());
        assertEquals("foo/bar/project-custom-name", projectIndex.getProjects().get(2).getPath());
        assertEquals(createDate(2023, 2, 28, 12, 34,14), projectIndex.getProjects().get(2).getModifiedAt());
        assertFalse(projectIndex.getProjects().get(2).isArchived());
    }

    private static Date createDate(int year, int month, int dayOfMonth, int hour, int minute, int seconds) {
        Calendar cal = new GregorianCalendar();
        cal.set(year, month - 1, dayOfMonth, hour, minute, seconds);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

}

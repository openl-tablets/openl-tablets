package org.openl.rules.webstudio.web.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;

public class RepositoryUtilsTest {

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();

    @BeforeAll
    public static void setUp() {
        Locale.setDefault(Locale.US);
        // set +2 as default
        TimeZone defaultTimeZone = TimeZone.getTimeZone("Europe/Helsinki");
        TimeZone.setDefault(defaultTimeZone);
    }

    @AfterAll
    public static void tearDown() {
        Locale.setDefault(DEFAULT_LOCALE);
        TimeZone.setDefault(DEFAULT_TIMEZONE);
    }

    @Test
    public void buildProjectVersionTest() {
        assertNull(RepositoryUtils.buildProjectVersion(null));
        FileData fileData = mock(FileData.class);

        Calendar cal = new GregorianCalendar();
        cal.set(2020, Calendar.AUGUST, 17, 11, 12, 13);
        cal.set(Calendar.MILLISECOND, 0);
        when(fileData.getModifiedAt()).thenReturn(cal.getTime());
        when(fileData.getAuthor()).thenReturn(new UserInfo("jsmith", "jsmith@email", "John Smith"));

        final String actual = RepositoryUtils.buildProjectVersion(fileData);
        assertEquals("John Smith-2020-08-17_11-12-13", actual);
    }
}

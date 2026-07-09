package org.openl.studio.projects.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class ProjectFilesControllerTest {

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();

    @BeforeAll
    static void setUp() {
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Helsinki"));
    }

    @AfterAll
    static void tearDown() {
        Locale.setDefault(DEFAULT_LOCALE);
        TimeZone.setDefault(DEFAULT_TIMEZONE);
    }

    @Test
    void projectArchiveNameUsesBusinessNameAndLastModifiedMetadata() {
        var fileData = mock(FileData.class);

        var calendar = new GregorianCalendar();
        calendar.set(2020, Calendar.AUGUST, 17, 11, 12, 13);
        calendar.set(Calendar.MILLISECOND, 0);
        when(fileData.getModifiedAt()).thenReturn(calendar.getTime());
        when(fileData.getAuthor()).thenReturn(new UserInfo("jsmith", "jsmith@email", "John Smith"));

        var actual = ProjectFilesController.getProjectArchiveName("Rules Service", fileData);

        assertEquals("Rules Service-John Smith-2020-08-17_11-12-13.zip", actual);
    }

    @Test
    void projectArchiveNameFallsBackToBusinessNameWhenMetadataIsMissing() {
        var actual = ProjectFilesController.getProjectArchiveName("Rules Service", null);

        assertEquals("Rules Service.zip", actual);
    }
}

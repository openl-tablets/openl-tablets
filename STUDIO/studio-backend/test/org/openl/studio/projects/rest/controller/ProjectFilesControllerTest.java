package org.openl.studio.projects.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;

class ProjectFilesControllerTest {

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();
    private static final ZoneId HELSINKI = ZoneId.of("Europe/Helsinki");
    private static final String FOLDER = "EPBDS";

    @BeforeAll
    static void setUp() {
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone(HELSINKI));
    }

    @AfterAll
    static void tearDown() {
        Locale.setDefault(DEFAULT_LOCALE);
        TimeZone.setDefault(DEFAULT_TIMEZONE);
    }

    @Test
    void namesTheArchiveAfterTheProjectAndTheMomentItWasWritten() {
        var actual = ProjectFilesController.getProjectArchiveName(
                "Rules Service", writtenAt(at(2020, Calendar.AUGUST, 17, 11, 12, 13)), HELSINKI);

        assertEquals("Rules Service-John Smith-2020-08-17_11-12-13.zip", actual);
    }

    @Test
    void namesTheArchiveAfterTheProjectAloneWhenNoMomentIsRecorded() {
        // A project whose folder has gone is reported as existing, with nothing but its name filled in.
        var nameless = new FileData();
        nameless.setName(FOLDER);

        assertEquals("Rules Service.zip",
                ProjectFilesController.getProjectArchiveName("Rules Service", nameless, HELSINKI));
        assertEquals("Rules Service.zip",
                ProjectFilesController.getProjectArchiveName("Rules Service", null, HELSINKI));
    }

    @Test
    void namesTheArchiveAfterTheRevisionItHolds() throws IOException {
        // The project stands on a later revision than the one asked for; the archive is named after what
        // it holds, not after where the project has moved on to.
        var design = repositoryHolding("older", at(2020, Calendar.AUGUST, 16, 9, 8, 7));
        var project = projectStandingOn(at(2020, Calendar.AUGUST, 17, 11, 12, 13), design);

        var actual = ProjectFilesController.getProjectArchiveName(project, "older", HELSINKI);

        assertEquals("Rules Service-John Smith-2020-08-16_09-08-07.zip", actual);
    }

    @Test
    void namesTheArchiveAfterTheProjectWhenNoRevisionIsAskedFor() throws IOException {
        var project = projectStandingOn(at(2020, Calendar.AUGUST, 17, 11, 12, 13), null);

        var actual = ProjectFilesController.getProjectArchiveName(project, null, HELSINKI);

        assertEquals("Rules Service-John Smith-2020-08-17_11-12-13.zip", actual);
    }

    @Test
    void namesTheArchiveAfterTheProjectAloneWhenNothingIsRecordedForTheRevision() throws IOException {
        // The download itself refuses a revision the repository holds nothing for; the name says nothing
        // about it either.
        var project = projectStandingOn(at(2020, Calendar.AUGUST, 17, 11, 12, 13), mock(Repository.class));

        var actual = ProjectFilesController.getProjectArchiveName(project, "gone", HELSINKI);

        assertEquals("Rules Service.zip", actual);
    }

    private static Date at(int year, int month, int day, int hour, int minute, int second) {
        var calendar = new GregorianCalendar();
        calendar.set(year, month, day, hour, minute, second);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private static FileData writtenAt(Date modifiedAt) {
        var fileData = mock(FileData.class);
        when(fileData.getModifiedAt()).thenReturn(modifiedAt);
        when(fileData.getAuthor()).thenReturn(new UserInfo("jsmith", "jsmith@email", "John Smith"));
        return fileData;
    }

    private static RulesProject projectStandingOn(Date modifiedAt, @Nullable Repository design) {
        // The file data is built before the stubbing starts: a mock made inside when(...) leaves that
        // stubbing unfinished.
        var fileData = writtenAt(modifiedAt);
        var project = mock(RulesProject.class);
        when(project.getBusinessName()).thenReturn("Rules Service");
        when(project.getFileData()).thenReturn(fileData);
        when(project.getDesignRepository()).thenReturn(design);
        when(project.getDesignFolderName()).thenReturn(FOLDER);
        return project;
    }

    /** A repository holding that revision of the project's folder. */
    private static Repository repositoryHolding(String version, Date modifiedAt) throws IOException {
        var older = writtenAt(modifiedAt);
        var repository = mock(Repository.class);
        when(repository.checkHistory(FOLDER, version)).thenReturn(older);
        return repository;
    }
}

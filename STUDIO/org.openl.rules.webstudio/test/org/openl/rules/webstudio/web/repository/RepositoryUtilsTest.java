package org.openl.rules.webstudio.web.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.BranchStatus;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.dtr.BranchedProject;
import org.openl.rules.workspace.dtr.DesignTimeRepository;

class RepositoryUtilsTest {

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();

    @BeforeAll
    static void setUp() {
        Locale.setDefault(Locale.US);
        // set +2 as default
        TimeZone defaultTimeZone = TimeZone.getTimeZone("Europe/Helsinki");
        TimeZone.setDefault(defaultTimeZone);
    }

    @AfterAll
    static void tearDown() {
        Locale.setDefault(DEFAULT_LOCALE);
        TimeZone.setDefault(DEFAULT_TIMEZONE);
    }

    @Test
    void buildProjectVersionTest() {
        assertNull(RepositoryUtils.buildProjectVersion(null));
        FileData fileData = mock(FileData.class);

        var cal = new GregorianCalendar();
        cal.set(2020, Calendar.AUGUST, 17, 11, 12, 13);
        cal.set(Calendar.MILLISECOND, 0);
        when(fileData.getModifiedAt()).thenReturn(cal.getTime());
        when(fileData.getAuthor()).thenReturn(new UserInfo("jsmith", "jsmith@email", "John Smith"));

        final String actual = RepositoryUtils.buildProjectVersion(fileData);
        assertEquals("John Smith-2020-08-17_11-12-13", actual);
    }

    @Test
    void getRepositoryForVersionUsesIndexedSecondaryBranchPath() throws Exception {
        var designRepository = mock(DesignTimeRepository.class);
        var baseRepository = mock(BranchRepository.class);
        var branchRepository = mock(BranchRepository.class);
        var branchProject = mock(AProject.class);
        var historicData = mock(FileData.class);

        when(baseRepository.getId()).thenReturn("design");
        when(baseRepository.supports()).thenReturn(new FeaturesBuilder(baseRepository).setBranches(true).build());
        when(baseRepository.checkHistory("rules/Pricing/", "revision-1")).thenReturn(null);
        when(branchProject.getRepository()).thenReturn(branchRepository);
        when(branchProject.getFolderPath()).thenReturn("mapped/Pricing");
        when(branchRepository.checkHistory("mapped/Pricing/", "revision-1")).thenReturn(historicData);
        when(designRepository.getBranchedProject("design", "Pricing"))
                .thenReturn(Optional.of(BranchedProject.create(
                        "Pricing",
                        "main",
                        Map.of("feature/rates",
                                new BranchedProject.BranchEntry(branchProject, mock(BranchStatus.class))))));

        var result = RepositoryUtils.getRepositoryForVersion(
                designRepository, baseRepository, "rules/", "Pricing", "revision-1");

        assertSame(branchRepository, result);
        verify(branchRepository).checkHistory("mapped/Pricing/", "revision-1");
    }
}

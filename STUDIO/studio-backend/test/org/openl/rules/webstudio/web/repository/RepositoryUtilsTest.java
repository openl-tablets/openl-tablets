package org.openl.rules.webstudio.web.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.jar.Manifest;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.ProjectFiles;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.BranchStatus;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
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
    void anArchiveToDeployLeavesTheProtectedFilesAndTheOldManifestOut() throws Exception {
        var repository = mock(Repository.class);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setFolders(true).build());
        var files = new ArrayList<FileData>();
        for (var name : List.of("rules.xml", "pom.xml", "META-INF/MANIFEST.MF", "rules/pom.xml")) {
            var file = new FileData();
            file.setName("rules/P/" + name);
            files.add(file);
        }
        when(repository.listFiles("rules/P/", "1")).thenReturn(List.copyOf(files));
        when(repository.readHistory(anyString(), any())).thenAnswer(call -> new FileItem(call.<String>getArgument(0),
                new ByteArrayInputStream(new byte[0])));
        var out = new ByteArrayOutputStream();

        RepositoryUtils.archive(mock(DesignTimeRepository.class), repository, "rules/", "P", "1", out, new Manifest(),
                ProjectFiles.of("pom.xml", null));

        var entries = new ArrayList<String>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                entries.add(entry.getName());
            }
        }
        // The manifest the deployment writes, not the one of the project.
        assertEquals(List.of("META-INF/MANIFEST.MF", "rules.xml", "rules/pom.xml"), entries);
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

        assertEquals("John Smith-2020-08-17_11-12-13", RepositoryUtils.buildProjectVersion(fileData));
        // The same moment, read by someone three hours behind: each is told the moment they would read
        // on their own screen rather than the one the machine stands in.
        assertEquals("John Smith-2020-08-17_08-12-13",
                RepositoryUtils.buildProjectVersion(fileData, ZoneOffset.UTC));
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

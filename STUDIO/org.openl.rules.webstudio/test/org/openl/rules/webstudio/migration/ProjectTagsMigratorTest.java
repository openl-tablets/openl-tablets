package org.openl.rules.webstudio.migration;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.StandardEnvironment;

import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;

/**
 * Tests of the migration of the project tags kept in the database before 6.0.0.
 *
 * @author Yury Molchan
 */
class ProjectTagsMigratorTest {

    private static final String PROJECT_PATH = "rules/Ex";
    private static final String TAGS_PATH = "rules/Ex/tags.properties";
    private static final String TAGS_CONTENT = "Environment=production\n";
    private static final String MODULE_ENTRY = "rules/Module.xlsx";
    /** A whole number of seconds, which is what an archive records. */
    private static final long MODULE_TIME = 1_700_000_000_000L;

    @TempDir
    Path tempDir;

    private LegacyTagsDatabase database;
    private final Map<String, byte[]> saved = new HashMap<>();
    private final Map<String, FileData> savedData = new HashMap<>();

    @BeforeEach
    void init() throws Exception {
        database = new LegacyTagsDatabase();
    }

    @AfterEach
    void shutDown() throws SQLException {
        database.close();
    }

    @Test
    void anInstallationWithoutADatabaseIsNotTouched() {
        var applicationContext = mock(ApplicationContext.class);
        when(applicationContext.containsBean("openlDataSource")).thenReturn(false);

        ProjectTagsMigrator.migrate(applicationContext);

        verify(applicationContext, never()).getBean(anyString(), any(Class.class));
    }

    @Test
    void absentLegacyTablesLeaveNothingToMigrate() throws SQLException {
        database.execute("DROP TABLE OpenL_Project_Tags");
        database.execute("DROP TABLE OpenL_Projects");
        var designTimeRepository = mock(DesignTimeRepository.class);

        ProjectTagsMigrator.migrate(applicationContext(designTimeRepository));

        verify(designTimeRepository, never()).getRepository(anyString());
    }

    @Test
    void theTagsOfAProjectOfAFolderRepositoryAreWrittenIntoIt() throws Exception {
        database.addTaggedProject("design", PROJECT_PATH);

        ProjectTagsMigrator.migrate(applicationContext(designRepository(folderRepository())));

        assertEquals(TAGS_CONTENT, new String(saved.get(TAGS_PATH), UTF_8));
        assertLegacyTablesAreDropped();
    }

    @Test
    void theTagsOfAProjectOfAnArchiveRepositoryAreWrittenIntoItsArchive() throws Exception {
        database.addTaggedProject("design", PROJECT_PATH);

        ProjectTagsMigrator.migrate(applicationContext(designRepository(archiveRepository())));

        var savedArchive = saved.get(PROJECT_PATH);
        assertEquals(savedArchive.length, savedData.get(PROJECT_PATH).getSize(),
                "The recorded size does not match the saved archive.");
        assertEquals("Tags in project rules/Ex were moved to tags.properties file",
                savedData.get(PROJECT_PATH).getComment());
        var archive = tempDir.resolve("saved.zip");
        Files.write(archive, savedArchive);
        try (var zip = new ZipFile(archive.toFile())) {
            var tags = zip.getEntry("tags.properties");
            assertNotNull(tags, "The saved archive has no tags file.");
            assertEquals(TAGS_CONTENT, new String(zip.getInputStream(tags).readAllBytes(), UTF_8));
            var module = zip.getEntry(MODULE_ENTRY);
            assertNotNull(module, "The saved archive lost the project itself.");
            assertEquals(MODULE_TIME, module.getTime(), "The saved archive lost the date of the project file.");
        }
        assertLegacyTablesAreDropped();
    }

    @Test
    void theTagsAreKeptWhenTheProjectIsNotAnArchive() throws Exception {
        database.addTaggedProject("design", PROJECT_PATH);
        var repository = repository(false);
        var projectData = new FileData();
        projectData.setName(PROJECT_PATH);
        when(repository.check(PROJECT_PATH)).thenReturn(projectData);
        when(repository.read(PROJECT_PATH))
                .thenAnswer(invocation -> new FileItem(projectData, new ByteArrayInputStream(new byte[0])));

        ProjectTagsMigrator.migrate(applicationContext(designRepository(repository)));

        verify(repository, never()).save(any(FileData.class), any(InputStream.class));
        assertLegacyTablesAreKept();
    }

    @Test
    void anArchiveThatAlreadyHasItsTagsIsLeftUntouched() throws Exception {
        database.addTaggedProject("design", PROJECT_PATH);
        var repository = archiveRepository("tags.properties", "Owner=team-a\n");

        ProjectTagsMigrator.migrate(applicationContext(designRepository(repository)));

        verify(repository, never()).save(any(FileData.class), any(InputStream.class));
        assertLegacyTablesAreDropped();
    }

    @Test
    void aProjectThatAlreadyHasItsTagsFileIsLeftUntouched() throws Exception {
        database.addTaggedProject("design", PROJECT_PATH);
        var repository = folderRepository();
        when(repository.check(TAGS_PATH)).thenReturn(new FileData());

        ProjectTagsMigrator.migrate(applicationContext(designRepository(repository)));

        verify(repository, never()).save(any(FileData.class), any(InputStream.class));
        assertLegacyTablesAreDropped();
    }

    @Test
    void anInstallationWithoutTaggedProjectsStillRetiresTheLegacyTables() {
        ProjectTagsMigrator.migrate(applicationContext(mock(DesignTimeRepository.class)));

        assertLegacyTablesAreDropped();
    }

    @Test
    void theTagsAreKeptWhenTheRepositoryOfAProjectDoesNotExist() throws SQLException {
        database.addTaggedProject("design", PROJECT_PATH);

        ProjectTagsMigrator.migrate(applicationContext(mock(DesignTimeRepository.class)));

        assertLegacyTablesAreKept();
    }

    @Test
    void theTagsAreKeptWhenTheRepositoryNoLongerHoldsTheProject() throws Exception {
        database.addTaggedProject("design", PROJECT_PATH);
        var repository = folderRepository();
        when(repository.check(PROJECT_PATH)).thenReturn(null);

        ProjectTagsMigrator.migrate(applicationContext(designRepository(repository)));

        assertLegacyTablesAreKept();
    }

    @Test
    void theTagsAreKeptWhenTheRepositoryIsUnreachable() throws Exception {
        database.addTaggedProject("design", PROJECT_PATH);
        var repository = folderRepository();
        when(repository.check(PROJECT_PATH)).thenThrow(new IOException("The repository is unreachable."));

        ProjectTagsMigrator.migrate(applicationContext(designRepository(repository)));

        assertLegacyTablesAreKept();
    }

    @Test
    void aMisconfiguredRepositoryNeitherStopsTheStartUpNorLosesTheTags() throws Exception {
        database.addTaggedProject("design", PROJECT_PATH);
        var repository = folderRepository();
        // A repository that failed to instantiate answers every call with an IllegalStateException.
        when(repository.check(PROJECT_PATH)).thenThrow(new IllegalStateException("Repository is incorrect."));

        ProjectTagsMigrator.migrate(applicationContext(designRepository(repository)));

        assertLegacyTablesAreKept();
    }

    private void assertLegacyTablesAreDropped() {
        assertFalse(tableExists("OpenL_Projects"), "OpenL_Projects is still there.");
        assertFalse(tableExists("OpenL_Project_Tags"), "OpenL_Project_Tags is still there.");
    }

    private void assertLegacyTablesAreKept() {
        assertTrue(tableExists("OpenL_Projects"), "OpenL_Projects is gone.");
        assertTrue(tableExists("OpenL_Project_Tags"), "OpenL_Project_Tags is gone.");
    }

    private boolean tableExists(String table) {
        try {
            return database.tableExists(table);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private ApplicationContext applicationContext(DesignTimeRepository designTimeRepository) {
        var applicationContext = mock(ApplicationContext.class);
        when(applicationContext.containsBean("openlDataSource")).thenReturn(true);
        when(applicationContext.getBean("openlDataSource", DataSource.class)).thenReturn(database.dataSource());
        when(applicationContext.getBean("designTimeRepository", DesignTimeRepository.class))
                .thenReturn(designTimeRepository);
        when(applicationContext.getEnvironment()).thenReturn(new StandardEnvironment());
        return applicationContext;
    }

    private static DesignTimeRepository designRepository(Repository repository) {
        var designTimeRepository = mock(DesignTimeRepository.class);
        when(designTimeRepository.getRepository("design")).thenReturn(repository);
        return designTimeRepository;
    }

    private Repository folderRepository() throws IOException {
        var repository = repository(true);
        when(repository.check(PROJECT_PATH)).thenReturn(new FileData());
        return repository;
    }

    private Repository archiveRepository() throws IOException {
        return archiveRepository(MODULE_ENTRY, "the rules of the project");
    }

    private Repository archiveRepository(String entry, String content) throws IOException {
        var repository = repository(false);
        var projectData = new FileData();
        projectData.setName(PROJECT_PATH);
        when(repository.check(PROJECT_PATH)).thenReturn(projectData);
        var archive = archiveOf(entry, content);
        when(repository.read(PROJECT_PATH))
                .thenAnswer(invocation -> new FileItem(projectData, new ByteArrayInputStream(archive)));
        return repository;
    }

    private Repository repository(boolean folders) throws IOException {
        var repository = mock(Repository.class);
        when(repository.getName()).thenReturn("Design");
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setFolders(folders).build());
        when(repository.save(any(FileData.class), any(InputStream.class))).thenAnswer(invocation -> {
            FileData data = invocation.getArgument(0);
            InputStream stream = invocation.getArgument(1);
            saved.put(data.getName(), stream.readAllBytes());
            savedData.put(data.getName(), data);
            return data;
        });
        return repository;
    }

    private static byte[] archiveOf(String entry, String content) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var zos = new ZipOutputStream(out)) {
            var zipEntry = new ZipEntry(entry);
            zipEntry.setTime(MODULE_TIME);
            zos.putNextEntry(zipEntry);
            zos.write(content.getBytes(UTF_8));
            zos.closeEntry();
        }
        return out.toByteArray();
    }
}

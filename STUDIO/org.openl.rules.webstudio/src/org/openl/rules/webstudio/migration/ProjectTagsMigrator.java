package org.openl.rules.webstudio.migration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.sql.DataSource;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ProjectTags;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RepositoryDelegate;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.util.PropertiesUtils;

/**
 * Moves the project tags the database still holds into the {@code tags.properties} file of each project.
 *
 * <p>Tags moved from the database into the file of each project in 6.0.0. Whether the installation is old
 * enough to still keep them in the database is decided by the caller. An installation without a database, and
 * one whose legacy tables are already gone, are left untouched.
 *
 * <p>The legacy tables are dropped once every project has its tags in its own file. A project the repository
 * does not hold keeps the tables, and with them its tags, instead of losing them.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class ProjectTagsMigrator {

    private static final String MIGRATION_COMMENT = "Tags in project %s were moved to tags.properties file";
    private static final String PATH_SEPARATOR = "/";

    private static final String MIGRATION_USER_NAME_PROPERTY = "migration.user.name";
    private static final String MIGRATION_USER_EMAIL_PROPERTY = "migration.user.email";
    private static final String DATA_SOURCE_BEAN = "openlDataSource";
    private static final String DESIGN_REPOSITORY_BEAN = "designTimeRepository";

    private final DesignTimeRepository designTimeRepository;
    private final UserInfo migrationUserInfo;

    /**
     * Moves the tags of every project the database still holds into the project itself.
     *
     * @param applicationContext the initialized application context
     */
    public static void migrate(ApplicationContext applicationContext) {
        if (!applicationContext.containsBean(DATA_SOURCE_BEAN)) {
            // The installation has no database at all.
            return;
        }
        try {
            var dataSource = applicationContext.getBean(DATA_SOURCE_BEAN, DataSource.class);
            var legacyTags = new LegacyProjectTags(dataSource);
            // The installation kept its tags in the projects from the start, or they are already moved there.
            if (legacyTags.exists()) {
                if (!migrateProjects(applicationContext, legacyTags.read())) {
                    log.error("The legacy tag tables are kept because the tags of some projects were not migrated.");
                } else {
                    legacyTags.drop();
                    log.info("The legacy tag tables are dropped.");
                }
            }
        } catch (Exception e) {
            // A migration that cannot run must not keep OpenL Studio from starting. The legacy tables, and
            // the tags in them, stay as they are, so nothing is lost.
            log.error("Migration of the project tags from the database failed.", e);
        }
    }

    /**
     * Writes the tags of the given projects into the projects themselves.
     *
     * @return whether every project now has its tags in its own file
     */
    private static boolean migrateProjects(ApplicationContext applicationContext,
                                           List<LegacyProjectTags.Project> projects) {
        if (projects.isEmpty()) {
            // Nothing is tagged, so the tables carry nothing worth keeping.
            return true;
        }
        var migrator = new ProjectTagsMigrator(
                applicationContext.getBean(DESIGN_REPOSITORY_BEAN, DesignTimeRepository.class),
                migrationUserInfo(applicationContext.getEnvironment()));
        var everyProjectMigrated = true;
        for (var project : projects) {
            everyProjectMigrated &= migrator.migrateProject(project);
        }
        return everyProjectMigrated;
    }

    private static UserInfo migrationUserInfo(Environment environment) {
        var migrationUsername = environment.getProperty(MIGRATION_USER_NAME_PROPERTY, "Studio Migration");
        var migrationUserEmail = environment.getProperty(MIGRATION_USER_EMAIL_PROPERTY, "");
        return new UserInfo(migrationUsername, migrationUserEmail, migrationUsername);
    }

    /**
     * Writes the tags of one project into the project itself.
     *
     * @return whether the tags are in the repository, which a project that could not be written answers with
     *         {@code false} so that its tags stay in the database
     */
    private boolean migrateProject(LegacyProjectTags.Project project) {
        log.info("Starting migration tags for project {} in repository {}",
                project.projectPath(),
                project.repositoryId());
        try {
            if (!writeTags(project.repositoryId(), project.projectPath(), project.tags())) {
                return false;
            }
            log.info("Successfully ended migration tags for project {} in repository {}",
                    project.projectPath(),
                    project.repositoryId());
            return true;
        } catch (Exception e) {
            // A repository that is misconfigured or unreachable throws whatever its implementation chooses, and
            // one broken project must neither stop the others nor take the tags of any of them down with it.
            log.error("Migration of project {} with repository id {} has failed.",
                    project.projectPath(),
                    project.repositoryId(),
                    e);
            return false;
        }
    }

    /**
     * Writes the tags of one project into its {@code tags.properties} file.
     *
     * <p>A project whose file is already there is left untouched and counts as written.
     *
     * @return whether the tags are in the repository
     */
    private boolean writeTags(String repositoryId,
                              String projectPath,
                              Map<String, String> projectTags) throws IOException, ProjectException {
        var repository = designTimeRepository.getRepository(repositoryId);
        if (repository == null) {
            log.warn("Tags of project {} are kept in the database because its repository {} does not exist.",
                    projectPath,
                    repositoryId);
            return false;
        }
        if (repository instanceof RepositoryDelegate repositoryDelegate) {
            repository = repositoryDelegate.getOriginal();
        }
        if (repository.check(projectPath) == null) {
            log.warn("Tags of project {}/{} are kept in the database because the repository no longer holds it.",
                    repository.getName(),
                    projectPath);
            return false;
        }
        try (var tags = new ByteArrayOutputStream()) {
            PropertiesUtils.store(tags, projectTags.entrySet());
            try (var tagsStream = new ByteArrayInputStream(tags.toByteArray())) {
                if (repository.supports().folders()) {
                    saveTagsInFolder(projectPath, repository, tagsStream);
                } else {
                    saveTagsInArchive(projectPath, repository, tagsStream);
                }
            }
        }
        return true;
    }

    private void saveTagsInFolder(String projectPath,
                                  Repository repository,
                                  InputStream tagsStream) throws IOException {
        var projectFolder = projectPath.endsWith(PATH_SEPARATOR) ? projectPath : projectPath + PATH_SEPARATOR;
        var fullTagsFileName = projectFolder + ProjectTags.TAGS_FILE_NAME;

        // If the file already exists, it means migration has already been done.
        if (repository.check(fullTagsFileName) != null) {
            log.info("Skipping saving tags for project {}/{} since file {} already exists",
                    repository.getName(),
                    projectFolder,
                    ProjectTags.TAGS_FILE_NAME);
            return;
        }
        log.info("Saving project tags for project {}/{}", repository.getName(), projectFolder);
        var tagsFileData = new FileData();
        tagsFileData.setName(fullTagsFileName);
        tagsFileData.setAuthor(migrationUserInfo);
        tagsFileData.setComment(MIGRATION_COMMENT.formatted(projectFolder));
        repository.save(tagsFileData, tagsStream);
    }

    private void saveTagsInArchive(String projectPath,
                                   Repository repository,
                                   InputStream tagsStream) throws IOException, ProjectException {
        try (var projectFileItem = repository.read(projectPath)) {
            var projectFileData = projectFileItem.getData();
            var rewritten = new ByteArrayOutputStream();
            if (copyWithTags(projectFileItem.getStream(), tagsStream, rewritten)) {
                log.info("Skipping saving tags for project {}/{} since archive entry {} already exists",
                        repository.getName(),
                        projectPath,
                        ProjectTags.TAGS_FILE_NAME);
                return;
            }
            var archive = rewritten.toByteArray();
            projectFileData.setAuthor(migrationUserInfo);
            projectFileData.setComment(MIGRATION_COMMENT.formatted(projectPath));
            projectFileData.setSize(archive.length);
            log.info("Saving archived project {}/{} with tags file", repository.getName(), projectPath);
            repository.save(projectFileData, new ByteArrayInputStream(archive));
        }
    }

    /**
     * Copies the project archive to the output and appends the tags file to it.
     *
     * <p>Every file of the project is carried over as it was, with the date it was last changed on, which
     * OpenL Studio reads back as the date of the file.
     *
     * <p>An archive is complete only once it is closed, so the output may be read only after this method
     * returns.
     *
     * @return whether the archive already holds a tags file, which leaves the output unusable
     * @throws ProjectException when the project cannot be read as an archive, which leaves it as it is
     */
    private static boolean copyWithTags(InputStream projectStream,
                                        InputStream tagsStream,
                                        OutputStream output) throws ProjectException {
        var copied = 0;
        try (var zis = new ZipInputStream(projectStream); var zos = new ZipOutputStream(output)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(ProjectTags.TAGS_FILE_NAME)) {
                    return true;
                }
                zos.putNextEntry(copyOf(entry));
                IOUtils.copy(zis, zos);
                zos.closeEntry();
                zis.closeEntry();
                copied++;
            }
            if (copied == 0) {
                // An empty stream and one that is not an archive both end the loop at once, and neither may be
                // replaced by an archive holding the tags alone.
                throw new ProjectException("The project holds no archive to write the tags into.");
            }
            zos.putNextEntry(new ZipEntry(ProjectTags.TAGS_FILE_NAME));
            IOUtils.copy(tagsStream, zos);
            zos.closeEntry();
            return false;
        } catch (IOException e) {
            throw new ProjectException(e.getMessage(), e);
        }
    }

    /**
     * An entry of the same name, carrying the date and the comment of the given one.
     */
    private static ZipEntry copyOf(ZipEntry entry) {
        var copy = new ZipEntry(entry.getName());
        if (entry.getTime() != -1) {
            copy.setTime(entry.getTime());
        }
        copy.setComment(entry.getComment());
        return copy;
    }
}

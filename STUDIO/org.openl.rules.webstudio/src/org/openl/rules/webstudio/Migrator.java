package org.openl.rules.webstudio;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import org.openl.rules.common.ProjectException;
import org.openl.rules.dataformat.yaml.YamlMapperFactory;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.project.impl.local.ProjectMetainfo;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.security.standalone.persistence.OpenLProject;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.webstudio.migration.ProjectTagsMigrator;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.admin.AdministrationSettings;
import org.openl.rules.webstudio.web.admin.security.NOPUserSettings;
import org.openl.rules.webstudio.web.install.KeyPairCertUtils;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.impl.ProjectIndex;
import org.openl.rules.workspace.dtr.impl.ProjectInfo;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.util.FileUtils;
import org.openl.util.PropertiesUtils;
import org.openl.util.StringUtils;

/**
 * For setting migration purposes. It cleans up default settings and reconfigure user defined properties.
 *
 * @author Yury Molchan
 */
@Slf4j
public class Migrator {

    private static final String MIGRATION_USER_NAME_PROPERTY = "migration.user.name";
    private static final String MIGRATION_USER_EMAIL_PROPERTY = "migration.user.email";
    private static final String REPOSITORY_PREFIX = "repository.";
    private static final String DEFAULT_COMMENT_ARCHIVE_SUFFIX = ".comment-template.user-message.default.archive";
    private static final String DEFAULT_COMMENT_DELETE_SUFFIX = ".comment-template.user-message.default.delete";
    private static final String DEFAULT_COMMENT_RESTORE_SUFFIX = ".comment-template.user-message.default.restore";
    private static final String DEFAULT_COMMENT_ERASE_SUFFIX = ".comment-template.user-message.default.erase";
    private static final String COMMENT_TEMPLATE_SUFFIX = ".comment-template";
    private static final String COMMENT_TEMPLATE_OLD_SUFFIX = ".comment-template-old";
    private static final String LOCAL_REPO_PATH_SUFFIX = ".local-repository-path";
    private static final String LEGACY_SINGLE_USERNAME = "DEFAULT";

    private Migrator() {
    }


    public static void migrate() {
        DynamicPropertySource settings = DynamicPropertySource.get();
        var props = new HashMap<String, String>();

        var fromVersion = settings.version();
        String stringFromVersion = fromVersion == null ? "5.23.1" : fromVersion;

        // add subsequent migrations in order of priority
        if (stringFromVersion.compareTo("5.24.0") < 0) {
            migrateTo5_24(settings, props);
        }
        if (stringFromVersion.compareTo("5.26.0") < 0) {
            migrateTo5_26_0(settings, props);
        }
        if (stringFromVersion.compareTo("5.26.1") < 0) {
            migrateTo5_26_1(settings, props);
        }
        if (stringFromVersion.compareTo("6.0.0") < 0) {
            migrateTo6_0_0(settings, props);
        }
        if (stringFromVersion.compareTo("6.3.1") < 0) {
            migrateTo6_4_0(settings, props);
        }
        // A single-user installation upgraded from before EPBDS-16213 keeps its workspace under the former
        // default user name; move it to the resolved name first, so the conversion below records the moved
        // projects and 6.4.0 does not read a fresh empty workspace while the previous one is abandoned.
        migrateSingleUserWorkspace();
        // The legacy .studioProps conversion is intentionally not guarded by the from-version. An env-var or
        // default installation keeps no dynamic settings file, so the from-version reads as the running build
        // (see DynamicPropertySource#loadProperties) and any version guard would skip the conversion. The
        // registry-first reconciliation on sign-in would then delete every unconverted legacy folder as a
        // stray one and destroy the user's uncommitted work. The conversion is idempotent and self-limiting,
        // so running it on every start is safe.
        migrateUserWorkspacesToMetainfoRegistry();

        if ("saml".equals(Props.text("user.mode"))) {
            // Generating required a private key and its certificate if they are missed
            // Due they should be unique and private per installation they cannot be defined in openl-default.properties
            // So it should be executed always there on startup
            // Introduced in 5.26
            if (Props.text("security.saml.local-key") == null || Props.text("security.saml.local-certificate") == null) {
                Pair<String, String> pair = KeyPairCertUtils.generateCertificate();
                if (pair != null) {
                    props.put("security.saml.local-key", pair.getKey());
                    props.put("security.saml.local-certificate", pair.getValue());
                }
            }
        }

        try {
            settings.save(props);
            settings.reloadIfModified();
        } catch (IOException e) {
            log.error("Migration of properties failed.", e);
        }
    }

    /**
     * Moves the single-user workspace to the currently resolved user name.
     *
     * <p>Before EPBDS-16213 the single user defaulted to {@code DEFAULT}; the default is now the OS account.
     * Without this move 6.4.0 reads a fresh empty workspace under the new name and leaves the previous work
     * behind. It runs only in single-user mode, only when the legacy workspace exists and the target does
     * not, so it is idempotent and never overwrites an existing workspace.
     */
    private static void migrateSingleUserWorkspace() {
        migrateSingleUserWorkspace(Props.text("user.mode"),
                Props.text(AdministrationSettings.USER_WORKSPACE_HOME),
                Props.text(NOPUserSettings.SINGLE_USERNAME));
    }

    static void migrateSingleUserWorkspace(@Nullable String userMode,
                                           @Nullable String workspacePath,
                                           @Nullable String username) {
        // Blank values must not fall through to Path.of(""), which resolves to the process working directory.
        if (!"single".equals(userMode) || StringUtils.isBlank(workspacePath) || StringUtils.isBlank(username)
                || LEGACY_SINGLE_USERNAME.equals(username)) {
            return;
        }
        var workspacesRoot = Path.of(workspacePath).normalize();
        var legacy = workspacesRoot.resolve(LEGACY_SINGLE_USERNAME);
        var target = workspacesRoot.resolve(username).normalize();
        // The user name is a configured path segment: an absolute or traversing value would move the
        // workspace outside its root, so reject anything that escapes it.
        if (!target.startsWith(workspacesRoot)) {
            log.warn("The single-user name '{}' resolves outside the workspace root; the move is skipped.", username);
            return;
        }
        if (!Files.isDirectory(legacy) || Files.exists(target)) {
            return;
        }
        try {
            Files.move(legacy, target);
            log.info("Moved the single-user workspace from '{}' to '{}'.", LEGACY_SINGLE_USERNAME, username);
        } catch (IOException e) {
            log.error("Failed to move the single-user workspace from '{}' to '{}'.", LEGACY_SINGLE_USERNAME, username, e);
        }
    }

    /**
     * Moves the legacy per-project {@code .studioProps} metainfo into the per-user metainfo registry.
     *
     * <p>Runs on every start and is idempotent: a project that already has a record is skipped, so the
     * conversion takes effect once regardless of how many times it is invoked.
     *
     * <p>A project folder with a missing or unreadable repository link gets no record: the registry is
     * authoritative, and such folders are deleted at the first workspace load. For linked projects the
     * legacy {@code .studioProps} folder and the in-project edit history are deleted right away.
     */
    private static void migrateUserWorkspacesToMetainfoRegistry() {
        String workspacePath = Props.text(AdministrationSettings.USER_WORKSPACE_HOME);
        // A blank path must not fall through to Path.of(""), which resolves to the process working directory.
        if (StringUtils.isNotBlank(workspacePath)) {
            migrateUserWorkspacesToMetainfoRegistry(Path.of(workspacePath));
        }
    }

    static void migrateUserWorkspacesToMetainfoRegistry(Path workspacesRoot) {
        if (!Files.isDirectory(workspacesRoot)) {
            return;
        }
        try (var userDirs = Files.list(workspacesRoot)) {
            userDirs.filter(Files::isDirectory)
                    .filter(dir -> !dir.getFileName().toString().startsWith("."))
                    .forEach(Migrator::migrateUserWorkspace);
        } catch (IOException e) {
            log.error("Migration of user workspaces failed.", e);
        }
    }

    private static void migrateUserWorkspace(Path userDir) {
        try (var projectDirs = Files.list(userDir)) {
            projectDirs.filter(Files::isDirectory)
                    .filter(dir -> !dir.getFileName().toString().startsWith("."))
                    .forEach(projectDir -> migrateProjectMetainfo(userDir, projectDir));
        } catch (IOException e) {
            log.error("Migration of the user workspace '{}' failed.", userDir, e);
        }
    }

    private static void migrateProjectMetainfo(Path userDir, Path projectDir) {
        var projectName = projectDir.getFileName().toString();
        if (MetainfoRegistry.exists(userDir, projectName)) {
            // Already migrated. A repeated run must not degrade the record to a local project.
            return;
        }
        try {
            var studioProps = projectDir.resolve(".studioProps");
            var metainfo = legacyMetainfo(studioProps.resolve(".version"),
                    legacyBaselines(studioProps.resolve("file-properties")));
            if (metainfo == null) {
                log.warn("""
                        The '{}' project has no repository link. The folder is deleted at the first\
                         workspace load.""", projectName);
                return;
            }
            MetainfoRegistry.store(userDir, projectName, metainfo);
            FileUtils.deleteQuietly(studioProps.toFile());
            FileUtils.deleteQuietly(projectDir.resolve(".history").toFile());
        } catch (IOException | RuntimeException e) {
            log.error("Migration of the '{}' project metainfo failed.", projectName, e);
        }
    }

    /**
     * Reads the legacy {@code .version} file. Returns {@code null} when the file is absent, unreadable,
     * or does not identify the source repository — such a project has no restorable link.
     */
    @Nullable
    private static ProjectMetainfo legacyMetainfo(Path versionFile, Map<String, ProjectMetainfo.FileBaseline> baselines) {
        if (!Files.isRegularFile(versionFile)) {
            return null;
        }
        var properties = new HashMap<String, String>();
        try {
            PropertiesUtils.load(versionFile, properties::put);
        } catch (IOException e) {
            log.warn("The '{}' file is unreadable.", versionFile, e);
            return null;
        }
        var repositoryId = properties.get("repository-id");
        if (repositoryId == null) {
            return null;
        }
        return new ProjectMetainfo(repositoryId,
                properties.get("path-in-repository"),
                properties.get("branch"),
                properties.get("version"),
                properties.get("author"),
                parseLongOrNull(properties.get("modified-at-long")),
                parseLongOrNull(properties.get("size")),
                properties.get("comment"),
                baselines);
    }

    private static Map<String, ProjectMetainfo.FileBaseline> legacyBaselines(Path filePropertiesDir) throws IOException {
        var baselines = new HashMap<String, ProjectMetainfo.FileBaseline>();
        if (!Files.isDirectory(filePropertiesDir)) {
            return baselines;
        }
        try (var stream = Files.walk(filePropertiesDir)) {
            for (Path file : (Iterable<Path>) stream.filter(Files::isRegularFile)::iterator) {
                var baseline = legacyBaseline(file);
                if (baseline != null) {
                    baselines.put("/" + filePropertiesDir.relativize(file).toString().replace('\\', '/'), baseline);
                }
            }
        }
        return baselines;
    }

    private static ProjectMetainfo.@Nullable FileBaseline legacyBaseline(Path propertiesFile) {
        var properties = new HashMap<String, String>();
        try {
            PropertiesUtils.load(propertiesFile, properties::put);
        } catch (IOException e) {
            log.warn("The '{}' file properties are unreadable and are skipped.", propertiesFile, e);
            return null;
        }
        Long size = parseLongOrNull(properties.get("size"));
        Long modifiedAt = parseLongOrNull(properties.get("modified-at-long"));
        if (size == null || modifiedAt == null) {
            // Without the baseline the file is later detected as locally changed, which is the safe side.
            return null;
        }
        return new ProjectMetainfo.FileBaseline(properties.get("unique-id"), size, modifiedAt);
    }

    @Nullable
    private static Long parseLongOrNull(@Nullable String value) {
        try {
            return value == null ? null : Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void migrateTo6_4_0(DynamicPropertySource settings, HashMap<String, String> props) {
        Arrays.stream(settings.getPropertyNames())
                .filter(propertyName -> propertyName.startsWith(REPOSITORY_PREFIX) && (propertyName
                        .endsWith(DEFAULT_COMMENT_ARCHIVE_SUFFIX) || propertyName
                        .endsWith(DEFAULT_COMMENT_DELETE_SUFFIX) || propertyName
                        .endsWith(DEFAULT_COMMENT_RESTORE_SUFFIX) || propertyName
                        .endsWith(DEFAULT_COMMENT_ERASE_SUFFIX) || propertyName
                        .endsWith(COMMENT_TEMPLATE_SUFFIX) || propertyName
                        .endsWith(COMMENT_TEMPLATE_OLD_SUFFIX)))
                .forEach(propertyName -> props.put(propertyName, null));
    }

    private static void migrateTo6_0_0(DynamicPropertySource settings, HashMap<String, String> props) {
        // remove `.folder-structure.flat` property
        Arrays.stream(settings.getPropertyNames())
                .filter(propertyName -> propertyName.startsWith(REPOSITORY_PREFIX) && propertyName.endsWith(".folder-structure.flat"))
                .forEach(propertyToRemove -> {
                    // remove property
                    props.put(propertyToRemove, null);
                });

        // Migrate '.local-repository-path' property to '.uri' if '.uri' is empty
        migrateLocalRepositoryPath(settings, props);
    }

    private static void migrateLocalRepositoryPath(DynamicPropertySource settings, HashMap<String, String> props) {
        Arrays.stream(settings.getPropertyNames())
                .filter(propertyName -> propertyName.startsWith(REPOSITORY_PREFIX)
                        && propertyName.endsWith(LOCAL_REPO_PATH_SUFFIX))
                .forEach(localRepositoryPathProperty -> {
                    var start = REPOSITORY_PREFIX.length();
                    var end = localRepositoryPathProperty.length() - LOCAL_REPO_PATH_SUFFIX.length();
                    var repositoryId = localRepositoryPathProperty.substring(start, end);
                    var uriPropName = REPOSITORY_PREFIX + repositoryId + ".uri";
                    var uri = settings.getProperty(uriPropName);
                    if (StringUtils.isEmpty(uri)) {
                        var localRepositoryPathValue = settings.getProperty(localRepositoryPathProperty);
                        props.put(uriPropName, localRepositoryPathValue);
                    }
                    props.put(localRepositoryPathProperty, null);
                });
    }

    private static void migrateTo5_26_1(DynamicPropertySource settings, HashMap<String, String> props) {
        migrateRepositoryFactories(settings, props);
        migrateProductionRepository(settings, props);
    }

    private static void migrateRepositoryFactories(DynamicPropertySource settings, HashMap<String, String> props) {
        var factorySuffix = ".factory";

        Arrays.stream(settings.getPropertyNames())
                .filter(propertyName -> propertyName.startsWith(REPOSITORY_PREFIX) && propertyName.endsWith(factorySuffix))
                .forEach(factoryKey -> {
                    var factory = settings.getProperty(factoryKey);
                    if (StringUtils.isNotBlank(factory)) {
                        var refKey = factoryKey.substring(0, factoryKey.length() - factorySuffix.length()) + ".$ref";
                        props.put(refKey, RepositoryInstatiator.getRefID(factory));
                        props.put(factoryKey, null);
                    }
                });
    }

    private static void migrateProductionRepository(DynamicPropertySource settings, HashMap<String, String> props) {
        // Production repository was mandatory in previous versions. In a new version defaults for it were removed.

        final var configListProp = "production-repository-configs";
        // Absent production repository configs assumes default setting: production-repository-configs = production
        var configList = settings.getProperty(configListProp);

        // Another case: production-repository-configs = production, production1, production2
        var repositories = Optional.ofNullable(configList).map(s -> Arrays
                .asList(StringUtils.split(s, ','))).orElse(List.of());
        var severalReposIncludingProduction = repositories.size() > 1 && repositories
                .contains("production");

        // Default Repository URI and Factory in the previous 5.26.0 version
        final var defaultUri = "jdbc:h2:mem:repo;DB_CLOSE_DELAY=-1";
        final var defaultFactory = "repo-jdbc";

        // Check, if URI for repository with id "production" was changed
        var repoUriProp = "repository.production.uri";
        var uri = settings.getProperty(repoUriProp);
        var factory = settings.getProperty("repository.production.factory");

        var repoIsChanged = uri != null && !uri.equals(defaultUri) || factory != null && !factory.equals(defaultFactory);

        // 1) If had only defaulted repository and its uri was not changed in configuration, we assume, it
        // wasn't used and can be removed in the latest OpenL Studio. Don't restore any defaults.
        // 2) If default repository was reconfigured (URI or factory were changed), then it was used,
        // we need to restore only absent defaults for repository with id "production".
        // 3) If several repositories existed but default repository with id "production" wasn't changed (including
        // URI), we restore all its defaults including URI.
        if (repoIsChanged || severalReposIncludingProduction) {
            if (configList == null) {
                // Restore default repository id
                props.put(configListProp, "production");
            }

            final var repoNameProp = "repository.production.name";
            if (!settings.containsProperty(repoNameProp)) {
                // Restore default repository name
                props.put(repoNameProp, "Deployment");
            }

            // Replace repository factory with repository ref.
            if (StringUtils.isBlank(factory)) {
                props.put("repository.production.$ref", defaultFactory);
            }

            // base.path is a mandatory setting for now, need to restore default value.
            props.put("repository.production.base.path.$ref", "repo-default.production.base.path");

            if (severalReposIncludingProduction && !repoIsChanged) {
                // Restore property as it was in previous OpenL Studio.
                props.put(repoUriProp, defaultUri);
            }
        }
    }

    // 5.26.0
    private static void migrateTo5_26_0(DynamicPropertySource settings, HashMap<String, String> props) {
        //removing unnecessary SAML properties
        props.put("security.saml.app-url", null);
        props.put("security.saml.authentication-contexts", null);
        props.put("security.saml.local-logout", null);
        props.put("security.saml.is-app-after-balancer", null);
        props.put("security.saml.scheme", null);
        props.put("security.saml.server-name", null);
        props.put("security.saml.server-port", null);
        props.put("security.saml.include-server-port-in-request-url", null);
        props.put("security.saml.context-path", null);
        props.put("security.saml.max-authentication-age", null);
        props.put("security.saml.metadata-trust-check", null);
        props.put("security.saml.request-timeout", null);
        props.put("security.saml.keystore-file-path", null);
        props.put("security.saml.keystore-password", null);
        props.put("security.saml.keystore-sp-alias", null);
        props.put("security.saml.keystore-sp-password", null);

        Arrays.stream(settings.getPropertyNames())
                .filter(propertyName -> propertyName.endsWith(".uri") || propertyName.endsWith(".url"))
                .map(settings::getProperty)
                .distinct()
                .forEach(uri -> {
                    if (uri != null && uri.startsWith("jdbc:h2:")) {
                        log.warn(
                                "You have h2 database with uri '{}'. Make sure that it's migrated to v2 or newer version. You need to migrate it yourself. See https://www.h2database.com/html/migration-to-v2.html for details.",
                                uri);
                    }
                });
    }

    // 5.24
    private static void migrateTo5_24(DynamicPropertySource settings, HashMap<String, String> props) {

        migratePropsTo5_24(settings, props);

        // migrate project paths and properties if repoType is Git
        var designRepo = settings.getProperty("repository.design.local-repository-path");
        var designRepoPath = designRepo != null ? designRepo : Props.text("openl.home") + "/design-repository";
        var nonFlatProjectPaths = loadProjectsPathes(designRepoPath);
        writeProjectPathesToYAML(nonFlatProjectPaths);
        // migrate NonFlat project settings
        migrateNonFlatProjectSettings(nonFlatProjectPaths);

        // migrate locks.
        migrateLocks(nonFlatProjectPaths);
    }

    private static Map<String, String> loadProjectsPathes(String designRepo) {
        var projectPathMap = new HashMap<String, String>();
        Path projectProperties = Path.of(designRepo, "openl-projects.properties");
        if (Files.isRegularFile(projectProperties)) {
            try {
                var projectProps = new HashMap<String, String>();
                PropertiesUtils.load(projectProperties, projectProps::put);
                var projectsCount = projectProps.size() / 2;
                for (var i = 1; i <= projectsCount; i++) {
                    var name = projectProps.get("project." + i + ".name");
                    var path = projectProps.get("project." + i + ".path");
                    projectPathMap.put(name, path);
                }
            } catch (IOException e) {
                log.error("Loading of openl-projects.properties has been failed.", e);
            }
        }
        return projectPathMap;
    }

    private static void migratePropsTo5_24(DynamicPropertySource settings, HashMap<String, String> props) {
        if (Props.bool("project.history.unlimited")) {
            props.put("project.history.count", ""); // Define unlimited
        }
        var runTestParallel = settings.getProperty("test.run.parallel");
        if (runTestParallel != null && !Boolean.parseBoolean(runTestParallel)) {
            props.put("test.run.thread.count", "1");
        }
        props.put("project.history.unlimited", null); // Remove
        props.put("test.run.parallel", null); // Remove
        props.put("project.history.home", null); // Remove

        // migrate design repository path
        var desRepo = settings.getProperty("repository.design.factory");
        if (settings.getProperty("repository.design.local-repository-path") == null && (desRepo == null || "repo-git"
                .equals(desRepo)) || "org.openl.rules.repository.git.GitRepository".equals(desRepo)) {
            props.put("repository.design.local-repository-path", "${openl.home}/design-repository");
        }

        // migrate design new-branch-pattern
        var desNewBranchPattern = settings.getProperty("repository.design.new-branch-pattern");
        if (desNewBranchPattern != null) {
            var migratedNewBranchPattern = desNewBranchPattern
                    .replace("{0}", "{project-name}")
                    .replace("{1}", "{username}")
                    .replace("{2}", "{current-date}");
            props.put("repository.design.new-branch.pattern", migratedNewBranchPattern);
            props.put("repository.design.new-branch-pattern", null);
        }
        rename(settings,
                props,
                "repository.design.comment-validation-pattern",
                "repository.design.comment-template.comment-validation-pattern");
        rename(settings,
                props,
                "repository.design.invalid-comment-message",
                "repository.design.comment-template.invalid-comment-message");

        // migrate deployment repository path
        var productionFactory = settings.getProperty("repository.production.factory");
        if (settings.getProperty("repository.production.local-repository-path") == null && ("repo-git".equals(
                productionFactory) || "org.openl.rules.repository.git.GitRepositoryrepo-git".equals(productionFactory))) {
            props.put("repository.production.local-repository-path", "${openl.home}/production-repository");
        }
    }

    private static void rename(DynamicPropertySource settings,
                               HashMap<String, String> props,
                               String oldKey,
                               String newKey) {
        if (settings.containsProperty(oldKey)) {
            var value = (String) settings.getProperty(oldKey);
            props.put(oldKey, null);
            props.put(newKey, value);
        }
    }

    private static void migrateNonFlatProjectSettings(Map<String, String> nonFlatProjectPaths) {
        String workspacePath = Props.text(AdministrationSettings.USER_WORKSPACE_HOME);
        Path workspace = Path.of(workspacePath);

        try {
            // depth 3 - WorkSpace/UserDir/ProjectName
            Files.walkFileTree(workspace, EnumSet.noneOf(FileVisitOption.class), 3, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    var version = dir.resolve(".studioProps/.version");
                    if (Files.isRegularFile(version)) {
                        var prName = dir.getFileName().toString();
                        var projectPath = nonFlatProjectPaths.getOrDefault(prName, "DESIGN/rules/" + prName);
                        Files.write(version,
                                ("\nrepository-id=design\npath-in-repository=" + projectPath + "\n").getBytes(),
                                StandardOpenOption.APPEND);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Migration of locks failed.", e);
        }
    }

    private static void writeProjectPathesToYAML(Map<String, String> projectPathMap) {
        if (projectPathMap.isEmpty()) {
            return;
        }

        var projects = new ArrayList<ProjectInfo>(projectPathMap.size());
        for (Map.Entry<String, String> entry : projectPathMap.entrySet()) {
            projects.add(new ProjectInfo(entry.getKey(), entry.getValue()));
        }
        var index = new ProjectIndex();
        index.setProjects(projects);
        Path config = Path.of(Props.text("openl.home"), "repositories/settings/design/openl-projects.yaml");
        createYaml(index, config);

    }

    private static void createYaml(Object data, Path filePath) {
        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, YamlMapperFactory.getYamlMapper().writeValueAsBytes(data));
        } catch (IOException e) {
            log.error("Writing to file has been failed.", e);
        }
    }

    private static void migrateLocks(Map<String, String> projectPathMap) {
        Path projectLocks = Path.of(Props.text(AdministrationSettings.USER_WORKSPACE_HOME), ".locks/rules");
        if (Files.exists(projectLocks)) {
            try {
                Files.walkFileTree(projectLocks, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        var lockPath = projectLocks.relativize(file);
                        var branchName = "";
                        // if lockPath does not contains lockBranchPath - repository has no branches
                        if (lockPath.startsWith("branches/")) {
                            // ./branches/{Project Name}/{branch/name}/{Project Name}
                            var branchPath = lockPath.subpath(2, lockPath.getNameCount() - 1);
                            branchName = "[branches]/" + branchPath;
                        }
                        var projectName = lockPath.getFileName().toString();
                        var projectPath = projectPathMap.getOrDefault(projectName, "/DESIGN/rules/" + projectName);
                        Path newLock = Path.of(Props.text(AdministrationSettings.USER_WORKSPACE_HOME),
                                ".locks/projects/design",
                                projectPath,
                                branchName,
                                "ready.lock");
                        newLock.getParent().toFile().mkdirs();
                        Files.copy(file, newLock);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                log.error("Migration of locks failed.", e);
            }
        }
    }

    private static <T> T runInSession(SessionFactory sessionFactory, Function<Session, T> consumer) {
        try (var session = sessionFactory.openSession()) {
            return consumer.apply(session);
        }
    }

    public static void migrateAfterContentInitialized(ApplicationContext applicationContext) {
        if (!applicationContext.containsBean("openlSessionFactory")) {
            //webstudio is not configured, skipping migration
            return;
        }
        var sessionFactory = (SessionFactory) applicationContext.getBean("openlSessionFactory");
        var allOpenLProjects = runInSession(sessionFactory, Migrator::readAllProjectsAndTags);
        if (!allOpenLProjects.isEmpty()) {
            var migrationUserInfo = createMigrationUserInfo(applicationContext.getEnvironment());
            var designTimeRepository = applicationContext.getBean("designTimeRepository", DesignTimeRepository.class);
            var migrator = new ProjectTagsMigrator(designTimeRepository);
            allOpenLProjects.forEach(openLProject -> {
                log.info("Starting migration tags for project {} in repository {}", openLProject.projectPath, openLProject.repositoryId);
                try {
                    migrator.migrate(openLProject.repositoryId, openLProject.projectPath, openLProject.tags, migrationUserInfo);
                    runInSession(sessionFactory, session -> deleteProjectTagsInDB(session, openLProject.id));
                    log.info("Successfully ended migration tags for project {} in repository {}", openLProject.projectPath, openLProject.repositoryId);
                } catch (IOException | ProjectException e) {
                    log.error("Migration of project %s with repository id %s has failed".formatted(openLProject.projectPath, openLProject.repositoryId), e);
                }
            });
        }
    }

    private static UserInfo createMigrationUserInfo(Environment environment) {
        var migrationUsername = environment.getProperty(MIGRATION_USER_NAME_PROPERTY, "Studio Migration");
        var migrationUserEmail = environment.getProperty(MIGRATION_USER_EMAIL_PROPERTY, "");
        return new UserInfo(migrationUsername, migrationUserEmail, migrationUsername);
    }

    @SuppressWarnings("deprecation")
    private static Void deleteProjectTagsInDB(Session session, Long id) {
        var transaction = session.beginTransaction();
        var openLProject = session.get(OpenLProject.class, id);
        session.remove(openLProject);
        transaction.commit();
        return null;
    }

    @SuppressWarnings("deprecation")
    private static List<OpenLProjectWithTags> readAllProjectsAndTags(Session session) {
        var cb = session.getCriteriaBuilder();
        var cq = cb.createQuery(OpenLProject.class);
        cq.from(OpenLProject.class);
        return session.createQuery(cq).getResultList()
                .stream()
                .map(openLProject ->
                        new OpenLProjectWithTags(
                                openLProject.getRepositoryId(),
                                openLProject.getProjectPath(),
                                openLProject.getId(),
                                openLProject.getTags().stream().collect(Collectors.toMap(tag -> tag.getType().getName(), Tag::getName))))
                .toList();

    }

    @RequiredArgsConstructor
    private static class OpenLProjectWithTags {
        private final String repositoryId;
        private final String projectPath;
        private final Long id;
        private final Map<String, String> tags;
    }
}

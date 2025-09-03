package org.openl.rules.webstudio;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.ProjectException;
import org.openl.rules.dataformat.yaml.YamlMapperFactory;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.git.branch.BranchesData;
import org.openl.rules.security.standalone.persistence.OpenLProject;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.webstudio.migration.ProjectTagsMigrator;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.admin.AdministrationSettings;
import org.openl.rules.webstudio.web.install.KeyPairCertUtils;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.impl.ProjectIndex;
import org.openl.rules.workspace.dtr.impl.ProjectInfo;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.util.PropertiesUtils;
import org.openl.util.StringUtils;

/**
 * For setting migration purposes. It cleans up default settings and reconfigure user defined properties.
 *
 * @author Yury Molchan
 */
public class Migrator {

    private static final String MIGRATION_USER_NAME_PROPERTY = "migration.user.name";
    private static final String MIGRATION_USER_EMAIL_PROPERTY = "migration.user.email";

    private Migrator() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(Migrator.class);

    public static void migrate() {
        DynamicPropertySource settings = DynamicPropertySource.get();
        HashMap<String, String> props = new HashMap<>();

        String fromVersion = settings.version();
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
            LOG.error("Migration of properties failed.", e);
        }
    }

    private static void migrateTo5_26_1(DynamicPropertySource settings, HashMap<String, String> props) {
        migrateRepositoryFactories(settings, props);
        migrateProductionRepository(settings, props);
    }

    private static void migrateRepositoryFactories(DynamicPropertySource settings, HashMap<String, String> props) {
        String factorySuffix = ".factory";

        Arrays.stream(settings.getPropertyNames())
                .filter(propertyName -> propertyName.startsWith("repository.") && propertyName.endsWith(factorySuffix))
                .forEach(factoryKey -> {
                    var factory = settings.getProperty(factoryKey);
                    if (StringUtils.isNotBlank(factory)) {
                        String refKey = factoryKey.substring(0, factoryKey.length() - factorySuffix.length()) + ".$ref";
                        props.put(refKey, RepositoryInstatiator.getRefID(factory));
                        props.put(factoryKey, null);
                    }
                });
    }

    private static void migrateProductionRepository(DynamicPropertySource settings, HashMap<String, String> props) {
        // Production repository was mandatory in previous versions. In a new version defaults for it were removed.

        final String configListProp = "production-repository-configs";
        // Absent production repository configs assumes default setting: production-repository-configs = production
        var configList = settings.getProperty(configListProp);

        // Another case: production-repository-configs = production, production1, production2
        List<String> repositories = Optional.ofNullable(configList).map(s -> Arrays
                .asList(StringUtils.split(s, ','))).orElse(Collections.emptyList());
        boolean severalReposIncludingProduction = repositories.size() > 1 && repositories
                .contains("production");

        // Default Repository URI and Factory in the previous 5.26.0 version
        final var defaultUri = "jdbc:h2:mem:repo;DB_CLOSE_DELAY=-1";
        final var defaultFactory = "repo-jdbc";

        // Check, if URI for repository with id "production" was changed
        String repoUriProp = "repository.production.uri";
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

            final String repoNameProp = "repository.production.name";
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
        Arrays.stream(settings.getPropertyNames())
                .filter(propertyName -> propertyName.startsWith("repository.") && propertyName.endsWith(".comment-template"))
                .forEach(propertyName -> {
                    var commentTemplate = settings.getProperty(propertyName);
                    if (commentTemplate != null && commentTemplate.contains("{username}")) {
                        props.put(propertyName, commentTemplate.replaceAll("(\\s+Author\\s*:?)?\\s*\\{username}\\.?", ""));
                    }
                });
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
                        LOG.warn(
                                "You have h2 database with uri '{}'. Make sure that it's migrated to v2 or newer version. You need to migrate it yourself. See https://www.h2database.com/html/migration-to-v2.html for details.",
                                uri);
                    }
                });
    }

    // 5.24
    private static void migrateTo5_24(DynamicPropertySource settings, HashMap<String, String> props) {

        migratePropsTo5_24(settings, props);

        // migrate branches and project properties to branches.yaml if repoType is Git
        var designRepo = settings.getProperty("repository.design.local-repository-path");
        var designRepoPath = designRepo != null ? designRepo : Props.text("openl.home") + "/design-repository";
        Map<String, String> nonFlatProjectPaths = loadProjectsPathes(designRepoPath);
        writeProjectPathesToYAML(nonFlatProjectPaths);
        migrateBranchesProps(nonFlatProjectPaths);

        // migrate NonFlat project settings
        migrateNonFlatProjectSettings(nonFlatProjectPaths);

        // migrate locks.
        migrateLocks(nonFlatProjectPaths);
    }

    private static Map<String, String> loadProjectsPathes(String designRepo) {
        Map<String, String> projectPathMap = new HashMap<>();
        Path projectProperties = Paths.get(designRepo, "openl-projects.properties");
        if (Files.isRegularFile(projectProperties)) {
            try {
                var projectProps = new HashMap<String, String>();
                PropertiesUtils.load(projectProperties, projectProps::put);
                int projectsCount = projectProps.size() / 2;
                for (int i = 1; i <= projectsCount; i++) {
                    String name = projectProps.get("project." + i + ".name");
                    String path = projectProps.get("project." + i + ".path");
                    projectPathMap.put(name, path);
                }
            } catch (IOException e) {
                LOG.error("Loading of openl-projects.properties has been failed.", e);
            }
        }
        return projectPathMap;
    }

    private static void migratePropsTo5_24(DynamicPropertySource settings, HashMap<String, String> props) {
        if (Props.bool("project.history.unlimited")) {
            props.put("project.history.count", ""); // Define unlimited
        }
        String runTestParallel = settings.getProperty("test.run.parallel");
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
            String migratedNewBranchPattern = desNewBranchPattern
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
            String value = (String) settings.getProperty(oldKey);
            props.put(oldKey, null);
            props.put(newKey, value);
        }
    }

    private static void migrateNonFlatProjectSettings(Map<String, String> nonFlatProjectPaths) {
        String workspacePath = Props.text(AdministrationSettings.USER_WORKSPACE_HOME);
        Path workspace = Paths.get(workspacePath);

        try {
            // depth 3 - WorkSpace/UserDir/ProjectName
            Files.walkFileTree(workspace, EnumSet.noneOf(FileVisitOption.class), 3, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path version = dir.resolve(".studioProps/.version");
                    if (Files.isRegularFile(version)) {
                        String prName = dir.getFileName().toString();
                        String projectPath = nonFlatProjectPaths.getOrDefault(prName, "DESIGN/rules/" + prName);
                        Files.write(version,
                                ("\nrepository-id=design\npath-in-repository=" + projectPath + "\n").getBytes(),
                                StandardOpenOption.APPEND);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOG.error("Migration of locks failed.", e);
        }
    }

    private static void migrateBranchesProps(Map<String, String> projectPathMap) {
        Path branchesProperties = Paths.get(Props.text("openl.home") + "/git-settings/branches.properties");
        if (Files.isRegularFile(branchesProperties)) {
            try {
                var branchProps = new HashMap<String, String>();
                PropertiesUtils.load(branchesProperties, branchProps::put);
                String numStr = branchProps.get("projects.number");
                BranchesData branches = new BranchesData();
                if (numStr != null) {
                    int num = Integer.parseInt(numStr);
                    for (int i = 1; i <= num; i++) {
                        String name = branchProps.get("project." + i + ".name");
                        String branchesStr = branchProps.get("project." + i + ".branches");
                        if (StringUtils.isBlank(name) || StringUtils.isBlank(branchesStr)) {
                            continue;
                        }
                        String namePath = projectPathMap.getOrDefault(name, "DESIGN/rules/" + name);
                        for (String branch : branchesStr.split(",")) {
                            branches.addBranch(namePath, branch, null);
                        }
                    }
                    Path config = Paths.get(Props.text("openl.home"), "repositories/settings/design/branches.yaml");
                    createYaml(branches, config);
                }
            } catch (IOException e) {
                LOG.error("Migration of branches.properties has been failed.", e);
            }
        }
    }

    private static void writeProjectPathesToYAML(Map<String, String> projectPathMap) {
        if (projectPathMap.isEmpty()) {
            return;
        }

        List<ProjectInfo> projects = new ArrayList<>(projectPathMap.size());
        for (Map.Entry<String, String> entry : projectPathMap.entrySet()) {
            projects.add(new ProjectInfo(entry.getKey(), entry.getValue()));
        }
        ProjectIndex index = new ProjectIndex();
        index.setProjects(projects);
        Path config = Paths.get(Props.text("openl.home"), "repositories/settings/design/openl-projects.yaml");
        createYaml(index, config);

    }

    private static void createYaml(Object data, Path filePath) {
        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, YamlMapperFactory.getYamlMapper().writeValueAsBytes(data));
        } catch (IOException e) {
            LOG.error("Writing to file has been failed.", e);
        }
    }

    private static void migrateLocks(Map<String, String> projectPathMap) {
        Path projectLocks = Paths.get(Props.text(AdministrationSettings.USER_WORKSPACE_HOME), ".locks/rules");
        if (Files.exists(projectLocks)) {
            try {
                Files.walkFileTree(projectLocks, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path lockPath = projectLocks.relativize(file);
                        String branchName = "";
                        // if lockPath does not contains lockBranchPath - repository has no branches
                        if (lockPath.startsWith("branches/")) {
                            // ./branches/{Project Name}/{branch/name}/{Project Name}
                            Path branchPath = lockPath.subpath(2, lockPath.getNameCount() - 1);
                            branchName = "[branches]/" + branchPath;
                        }
                        String projectName = lockPath.getFileName().toString();
                        String projectPath = projectPathMap.getOrDefault(projectName, "/DESIGN/rules/" + projectName);
                        Path newLock = Paths.get(Props.text(AdministrationSettings.USER_WORKSPACE_HOME),
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
                LOG.error("Migration of locks failed.", e);
            }
        }
    }
    
    public static void migrateAfterContentInitialized(ApplicationContext applicationContext) {
        if (! applicationContext.containsBean("openlSessionFactory")) {
            //webstudio is not configured, skipping migration
            return;
        }
        SessionFactory sessionFactory = (SessionFactory) applicationContext.getBean("openlSessionFactory");
        try (Session session = sessionFactory.openSession()) {
            List<OpenLProject> allOpenLProjects = readAllProjectsAndTags(session);
            if (!allOpenLProjects.isEmpty()) {
                var migrationUserInfo = createMigrationUser(applicationContext.getEnvironment());
                var designTimeRepository = applicationContext.getBean("designTimeRepository", DesignTimeRepository.class);
                var migrator = new ProjectTagsMigrator(designTimeRepository);
                allOpenLProjects.forEach(openLProject -> {
                    var projectTags = openLProject.getTags().stream().collect(Collectors.toMap(tag -> tag.getType().getName(), Tag::getName));
                    try {
                        migrator.migrate(openLProject.getRepositoryId(), openLProject.getProjectPath(), projectTags, migrationUserInfo);
                        deleteProjectTagsInDB(openLProject, session);
                    } catch (IOException | ProjectException e) {
                        LOG.error(String.format("Migration of project %s with repository id %s has failed", openLProject.getProjectPath(), openLProject.getRepositoryId()), e);
                    }
                });
            }

        }
    }

    private static CommonUser createMigrationUser(Environment environment) {
        String migrationUsername = environment.getProperty(MIGRATION_USER_NAME_PROPERTY, "Studio Migration");
        String migrationUserEmail = environment.getProperty(MIGRATION_USER_EMAIL_PROPERTY, "");
        UserInfo systemUserInfo = new UserInfo(migrationUsername, migrationUserEmail, migrationUsername);
        return new CommonUser() {
            @Override
            public String getUserName() {
                return migrationUsername;
            }

            @Override
            public UserInfo getUserInfo() {
                return systemUserInfo;
            }
        };
    }

    private static void deleteProjectTagsInDB(OpenLProject openLProject, Session session) {
        Transaction transaction = session.beginTransaction();
        session.delete(openLProject);
        transaction.commit();
    }

    private static List<OpenLProject> readAllProjectsAndTags(Session session) {
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<OpenLProject> cq = cb.createQuery(OpenLProject.class);
        cq.from(OpenLProject.class);
        return session.createQuery(cq).getResultList();

    }
}

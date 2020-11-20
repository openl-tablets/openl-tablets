package org.openl.rules.webstudio;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.openl.info.OpenLVersion;
import org.openl.rules.repository.git.branch.BranchesData;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.admin.AdministrationSettings;
import org.openl.rules.workspace.dtr.impl.ProjectIndex;
import org.openl.rules.workspace.dtr.impl.ProjectInfo;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * For setting migration purposes. It cleans up default settings and reconfigure user defined properties.
 *
 * @author Yury Molchan
 */
public class Migrator {

    private Migrator() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(Migrator.class);

    public static void migrate() {
        DynamicPropertySource settings = DynamicPropertySource.get();
        if (!settings.getFile().exists()) {
            // first start on the current directory, no migration needed
            return;
        }
        HashMap<String, String> props = new HashMap<>();

        Object fromVersion = settings.getProperty(".version");
        String stringFromVersion = fromVersion == null ? "5.23.1" : fromVersion.toString();

        // add subsequent migrations in order of priority
        if (stringFromVersion.compareTo("5.24.0") < 0) {
            migrateTo5_24(settings, props);
        }

        props.put(".version", OpenLVersion.getVersion()); // Mark the file version
        try {
            settings.save(props);
            settings.reloadIfModified();
        } catch (IOException e) {
            LOG.error("Migration of properties failed.", e);
        }
    }

    // 5.24
    private static void migrateTo5_24(DynamicPropertySource settings, HashMap<String, String> props) {

        migratePropsTo5_24(settings, props);

        // migrate branches and project properties to branches.yaml if repoType is Git
        Object designRepo = settings.getProperty("repository.design.local-repository-path");
        String designRepoPath = designRepo != null ? designRepo.toString()
                                                   : Props.text("openl.home") + "/design-repository";
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
            try (Reader in = Files.newBufferedReader(projectProperties, StandardCharsets.UTF_8)) {
                Properties projectProps = new Properties();
                projectProps.load(in);
                int projectsCount = projectProps.size() / 2;
                for (int i = 1; i <= projectsCount; i++) {
                    String name = projectProps.getProperty("project." + i + ".name");
                    String path = projectProps.getProperty("project." + i + ".path");
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
        Object runTestParallel = settings.getProperty("test.run.parallel");
        if (runTestParallel != null && !Boolean.parseBoolean(runTestParallel.toString())) {
            props.put("test.run.thread.count", "1");
        }
        props.put("project.history.unlimited", null); // Remove
        props.put("test.run.parallel", null); // Remove
        props.put("project.history.home", null); // Remove

        // migrate deploy-config
        if (("true").equals(settings.getProperty("repository.deploy-config.separate-repository"))) {
            props.put("repository.deploy-config.separate-repository", null);
            props.put("repository.deploy-config.use-repository", null);

            // migrate local repo path if have default value, since the default has changed on 5.24.0
            // null means this property have default value from previous OpenL version
            Object depConfRepo = settings.getProperty("repository.deploy-config.factory");
            if (settings.getProperty(
                "repository.deploy-config.local-repository-path") == null && (depConfRepo == null || "org.openl.rules.repository.git.GitRepository"
                    .equals(depConfRepo))) {
                props.put("repository.deploy-config.local-repository-path", "${openl.home}/deploy-config-repository");
            }
        } else {
            props.put("repository.deploy-config.use-repository", "design");
        }

        // migrate design repository path
        Object desRepo = settings.getProperty("repository.design.factory");
        if (settings.getProperty(
            "repository.design.local-repository-path") == null && (desRepo == null || "org.openl.rules.repository.git.GitRepository"
                .equals(desRepo))) {
            props.put("repository.design.local-repository-path", "${openl.home}/design-repository");
        }

        // migrate design new-branch-pattern
        Object desNewBranchPattern = settings.getProperty("repository.design.new-branch-pattern");
        if (desNewBranchPattern != null) {
            String migratedNewBranchPattern = desNewBranchPattern.toString()
                    .replace("{0}", "{project-name}")
                    .replace("{1}", "{username}")
                    .replace("{2}", "{current-date}");
            props.put("repository.design.new-branch-pattern", migratedNewBranchPattern);
        }

        // migrate deployment repository path
        if (settings.getProperty(
            "repository.production.local-repository-path") == null && "org.openl.rules.repository.git.GitRepository"
                .equals(settings.getProperty("repository.production.factory"))) {
            props.put("repository.production.local-repository-path", "${openl.home}/production-repository");
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
            try (Reader in = Files.newBufferedReader(branchesProperties, StandardCharsets.UTF_8)) {
                Properties branchProps = new Properties();
                branchProps.load(in);
                String numStr = branchProps.getProperty("projects.number");
                BranchesData branches = new BranchesData();
                if (numStr != null) {
                    int num = Integer.parseInt(numStr);
                    for (int i = 1; i <= num; i++) {
                        String name = branchProps.getProperty("project." + i + ".name");
                        String branchesStr = branchProps.getProperty("project." + i + ".branches");
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
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        String dump = yaml.dump(data);
        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, dump.getBytes(StandardCharsets.UTF_8));
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
                            if (branchPath != null) {
                                branchName = "[branches]/" + branchPath;
                            }
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
}

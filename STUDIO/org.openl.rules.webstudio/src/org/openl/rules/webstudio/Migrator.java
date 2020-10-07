package org.openl.rules.webstudio;

import org.openl.info.OpenLVersion;
import org.openl.rules.repository.git.branch.BranchesData;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.admin.AdministrationSettings;
import org.openl.rules.webstudio.web.admin.RepositoryType;
import org.openl.rules.workspace.dtr.impl.ProjectIndex;
import org.openl.rules.workspace.dtr.impl.ProjectInfo;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

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
            return;
        }
        HashMap<String, String> props = new HashMap<>();

        Object fromVersion = settings.getProperty(".version");

        //fromVersion == null means that this is the first start, no migration needed
        if (fromVersion != null) {
            String stringFromVersion = fromVersion.toString();
            if (fromVersion.toString().compareTo(OpenLVersion.getVersion()) < 0) {
                migrateTo5_23_5(props);
                migrateTo5_24(settings, props, stringFromVersion);
                //add subsequent migrations in order of priority
            }
        }

        props.put(".version", OpenLVersion.getVersion()); // Mark the file version
        try {
            settings.save(props);
            settings.reloadIfModified();
        } catch (IOException e) {
            LOG.error("Migration of properties failed.", e);
        }
    }

    //5.23.5
    private static void migrateTo5_23_5(HashMap<String, String> props) {
        if (Props.bool("project.history.unlimited")) {
            props.put("project.history.count", ""); // Define unlimited
        }
        props.put("project.history.unlimited", null); // Remove
        props.put("test.run.parallel", null); // Remove
        props.put("project.history.home", null); // Remove
    }

    //5.24
    private static void migrateTo5_24(DynamicPropertySource settings, HashMap<String, String> props, String fromVersion) {
        if (fromVersion.compareTo("5.24.0") < 0) {
            try {
                // migrate local repo path if have default value, since the default has changed on 5.24.0
                // null means this property have default value from previous OpenL version
                Object objDesignRepo = settings.getProperty("repository.design.local-repository-path");
                String homePath = Props.text("openl.home");
                String designRepo = objDesignRepo != null ? objDesignRepo.toString() : homePath + "\\design-repository";
                if (objDesignRepo == null) {
                    props.put("repository.design.local-repository-path", designRepo);
                }

                // migrate deploy-config
                if (settings.getProperty("repository.deploy-config.separate-repository") == null) {
                    props.put("repository.deploy-config.use-repository", "design");
                }

                // migrate deploy-config
                if (settings.getProperty("repository.production.local-repository-path") == null) {
                    props.put("repository.production.local-repository-path", Props.text("openl.home") + "\\production-repository");
                }

                //migrate branches and project properties to branches.yaml if repoType is Git
                Map<String, String> stringStringMap = migrateProjectProps(designRepo);
                migrateBranchesProps(stringStringMap);

                //migrate locks.
                migrateLocks(stringStringMap, homePath);
            } catch (IOException e) {
                LOG.error("Migration failed.", e);
            }
        }
    }

    private static void migrateBranchesProps(Map<String, String> projectPathMap) {
        if (RepositoryType.GIT.getFactoryClassName().equals(Props.text("repository.design.factory"))) {
            File branchesProperties = new File(new File(Props.text("openl.home") + "\\git-settings"), "branches.properties");
            if (branchesProperties.isFile()) {
                try (InputStreamReader in = new InputStreamReader(new FileInputStream(branchesProperties),
                        StandardCharsets.UTF_8)) {
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
                            for (String branch : branchesStr.split(",")) {
                                String namePath = projectPathMap.get(name) != null ? projectPathMap.get(name) : "\\DESIGN\\rules\\" + name;
                                branches.addBranch(namePath, branch, null);
                            }
                        }
                        createYaml(branches, Paths.get(Props.text("openl.home"), "repositories", "settings", "design", "branches.yaml"));
                    }
                } catch (IOException e) {
                    LOG.error("Migration of branches properties failed.", e);
                }
            }
        }
    }

    private static Map<String, String> migrateProjectProps(String designRepo) {
        Map<String, String> projectPathMap = new HashMap<>();
        if (RepositoryType.GIT.getFactoryClassName().equals(Props.text("repository.design.factory"))) {
            File projectProperties = new File(designRepo, "openl-projects.properties");
            if (projectProperties.isFile()) {
                try (InputStreamReader in = new InputStreamReader(new FileInputStream(projectProperties),
                        StandardCharsets.UTF_8)) {
                    Properties projectProps = new Properties();
                    projectProps.load(in);
                    int projectsCount = projectProps.size() / 2;
                    ProjectIndex index = new ProjectIndex();
                    List<ProjectInfo> projects = new ArrayList<>();
                    for (int i = 1; i <= projectsCount; i++) {
                        String name = projectProps.getProperty("project." + i + ".name");
                        String path = projectProps.getProperty("project." + i + ".path");
                        projects.add(new ProjectInfo(name, path));
                        projectPathMap.put(name, path);
                    }
                    index.setProjects(projects);
                    createYaml(index, Paths.get(Props.text("openl.home"), "repositories", "settings", "design", "openl-projects.yaml"));
                } catch (IOException e) {
                    LOG.error("Migration of project properties failed.", e);
                }
            }
        }
        return projectPathMap;
    }


    private static void createYaml(Object data, Path filePath) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (OutputStreamWriter out = new OutputStreamWriter(outputStream,
                StandardCharsets.UTF_8)) {
            yaml.dump(data, out);
        }
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                outputStream.toByteArray());
        File file = filePath.toFile();
        File parentFile = file.getParentFile();
        if (!parentFile.mkdirs() && !parentFile.exists()) {
            throw new FileNotFoundException(
                    "Cannot create the folder " + parentFile.getAbsolutePath());
        }
        IOUtils.copyAndClose(byteArrayInputStream, new FileOutputStream(file));
    }

    private static void migrateLocks(Map<String, String> projectPathMap, String homePath) throws IOException {
        File projectLocks = Paths.get(Props.text(AdministrationSettings.USER_WORKSPACE_HOME), ".locks", "rules").toFile();
        String lockPath = homePath + "\\user-workspace\\.locks\\rules\\branches\\";
        if (projectLocks.exists() && projectLocks.isDirectory()) {
            Files.walkFileTree(projectLocks.toPath(), new HashSet<>(), 50, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    File lock = file.toFile();
                    if (lock.isFile()) {
                        String branchName = lock.getPath().substring(lockPath.length()).replaceFirst(lock.getName() + "\\\\", "").replaceAll("\\\\" + lock.getName() + "$", "");
                        String fileName = lock.getName();
                        String projectName = projectPathMap.get(fileName) != null ? projectPathMap.get(fileName) : "\\DESIGN\\rules\\" + fileName;
                        Path newLock = Paths.get(Props.text(AdministrationSettings.USER_WORKSPACE_HOME), ".locks", "projects", "design", projectName, "[branches]", branchName, "ready.lock");
                        FileUtils.copy(lock, newLock.toFile());
                    }
                    return super.visitFile(file, attrs);
                }
            });
        }
    }
    //
}

package org.openl.rules.webstudio;

import org.openl.info.OpenLVersion;
import org.openl.rules.repository.git.branch.BranchesData;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.admin.AdministrationSettings;
import org.openl.rules.webstudio.web.admin.RepositoryType;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

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
    private static void  migrateTo5_23_5(HashMap<String, String> props){
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
                if (settings.getProperty("repository.design.local-repository-path") == null) {
                    props.put("repository.design.local-repository-path", Props.text("openl.home") + "\\design-repository");
                }

                // migrate deploy-config
                if (settings.getProperty("repository.deploy-config.separate-repository") == null) {
                    props.put("repository.deploy-config.use-repository", "design");
                }

                //migrate branches.properties to branches.yaml if repoType is Git
                migrateBranchesProps();

                //migrate locks.
                migrateLocks();
            } catch (IOException e) {
                LOG.error("Migration failed.", e);
            }
        }
    }

    private static void migrateBranchesProps() {
        if (RepositoryType.GIT.getFactoryClassName().equals(Props.text("repository.design.factory"))) {
            File branchesProperties = new File(new File(Props.text("openl.home") + "\\git-settings"), "branches.properties");
            if (branchesProperties.isFile()) {
                try (InputStreamReader in = new InputStreamReader(new FileInputStream(branchesProperties),
                        StandardCharsets.UTF_8)) {
                    Properties properties = new Properties();
                    properties.load(in);
                    String numStr = properties.getProperty("projects.number");
                    BranchesData branches = new BranchesData();
                    if (numStr != null) {
                        int num = Integer.parseInt(numStr);
                        for (int i = 1; i <= num; i++) {
                            String name = properties.getProperty("project." + i + ".name");
                            String branchesStr = properties.getProperty("project." + i + ".branches");
                            if (StringUtils.isBlank(name) || StringUtils.isBlank(branchesStr)) {
                                continue;
                            }
                            for (String branch : branchesStr.split(",")) {
                                branches.addBranch(name, branch, null);
                            }
                        }
                        DumperOptions options = new DumperOptions();
                        options.setPrettyFlow(true);
                        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                        Yaml yaml = new Yaml(options);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        try (OutputStreamWriter out = new OutputStreamWriter(outputStream,
                                StandardCharsets.UTF_8)) {
                            yaml.dump(branches, out);
                        }
                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                                outputStream.toByteArray());
                        File file = new File(Props.text("openl.home") + "\\repositories\\settings\\design\\", "branches.yaml");
                        File parentFile = file.getParentFile();
                        if (!parentFile.mkdirs() && !parentFile.exists()) {
                            throw new FileNotFoundException(
                                    "Cannot create the folder " + parentFile.getAbsolutePath());
                        }
                        IOUtils.copyAndClose(byteArrayInputStream, new FileOutputStream(file));
                    }
                } catch (IOException e) {
                    LOG.error("Migration of branches properties failed.", e);
                }
            }
        }
    }

    private static void migrateLocks() throws IOException {
        File projectLocks = Paths.get(Props.text(AdministrationSettings.USER_WORKSPACE_HOME), ".locks", "rules").toFile();
        if (projectLocks.exists() && projectLocks.isDirectory()) {
            Files.walkFileTree(projectLocks.toPath(), new HashSet<>(), 4, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    File lock = file.toFile();
                    if (lock.isFile()) {
                        File branch = lock.getParentFile();
                        File project = branch.getParentFile();
                        Path newLock = Paths.get(Props.text(AdministrationSettings.USER_WORKSPACE_HOME), ".locks", "projects", "design", project.getName(), ".branches", branch.getName(), "ready.lock");
                        FileUtils.copy(lock, newLock.toFile());
                    }
                    return super.visitFile(file, attrs);
                }
            });
        }
    }
    //
}

package org.openl.rules.webstudio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Properties;

import org.openl.info.OpenLVersion;
import org.openl.rules.repository.git.branch.BranchesData;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.admin.RepositoryType;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.util.IOUtils;
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

        HashMap<String, String> props = new HashMap<>();

        Object fromVersion = settings.getProperty(".version");

        if (fromVersion != null && fromVersion.toString().compareTo("5.24.0") < 0) {
            String openlHome = Props.text("openl.home");
            // migrate local repo path if have default value, since the default has changed on 5.24.0
            if (settings.getProperty("repository.design.local-repository-path") == null) {
                props.put("repository.design.local-repository-path", openlHome + "\\design-repository");
            }

            //migrate branches.properties to branches.yaml if repoType is Git
            if (RepositoryType.GIT.getFactoryClassName().equals(Props.text("repository.design.factory"))) {
                File branchesProperties = new File(new File(openlHome + "\\git-settings"), "branches.properties");
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
                            File file = new File(openlHome + "\\repositories\\settings\\design\\", "branches.yaml");
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

        if (Props.bool("project.history.unlimited")) {
            props.put("project.history.count", ""); // Define unlimited
        }
        props.put("project.history.unlimited", null); // Remove
        props.put("test.run.parallel", null); // Remove
        props.put("project.history.home", null); // Remove
        props.put(".version", OpenLVersion.getVersion()); // Mark the file version
        try {
            settings.save(props);
            settings.reloadIfModified();
        } catch (IOException e) {
            LOG.error("Migration of properties failed.", e);
        }
    }
}

package org.openl.rules.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.Properties;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.openl.rules.ruleservice.deployer.RulesDeployerService;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;

/**
 * Created by dl on 6/15/17.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY, requiresDependencyResolution = ResolutionScope.COMPILE)
public class DeployMojo extends BaseOpenLMojo {

    @Parameter(property = "openl.deployServer", required = true)
    private String deployServer;

    @Parameter(property = "openl.deployUrl", required = true)
    private String deployUrl;

    @Parameter(property = "openl.deployType", required = true)
    private String deployType;

    @Parameter(defaultValue = "${project.build.finalName}", readonly = true)
    private String finalName;

    /**
     * Directory containing the generated artifact.
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${settings}", readonly = true)
    private Settings settings;

    @Override
    void execute(String sourcePath, boolean hasDependencies) throws Exception {
        if (StringUtils.isEmpty(deployServer)) {
            throw new IllegalArgumentException("The deploy server name cannot be empty");
        }
        if (StringUtils.isEmpty(deployUrl)) {
            throw new IllegalArgumentException("The deploy server url cannot be empty");
        }

        File zipFile = findZipFile();

        Server server = settings.getServer(deployServer);
        if (server == null) {
            throw new IllegalStateException(
                String.format("The server configuration with name %s does not exist", deployServer));
        }

        Properties properties = new Properties();
        if ("jdbc".equals(deployType)) {
            properties.put("repository.production.factory", "org.openl.rules.repository.db.JdbcDBRepositoryFactory");
        }
        properties.put("repository.production.uri", deployUrl);
        properties.put("repository.production.login", server.getUsername());
        properties.put("repository.production.password", server.getPassword());
        properties.put("repository.production.base.path", "deploy/");
        //todo: change logic there
        try (RulesDeployerService deployerService = new RulesDeployerService(null)) {
            deployerService.deploy(FileUtils.getBaseName(zipFile.getName()), new FileInputStream(zipFile), false);
        }
    }

    private File findZipFile() {
        File[] zipZiles = outputDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(finalName) && name.endsWith(".zip");
            }
        });
        if (zipZiles == null) {
            throw new IllegalStateException("Cannot deploy the rules project, as the zip file does not exist");
        }
        if (zipZiles.length > 1) {
            throw new IllegalStateException("There are more than 1 zip file in the target directory");
        }
        return zipZiles[0];
    }
}

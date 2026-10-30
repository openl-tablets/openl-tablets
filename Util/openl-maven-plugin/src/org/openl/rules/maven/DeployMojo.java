package org.openl.rules.maven;

import java.io.File;
import java.util.HashMap;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.settings.Settings;

import org.openl.rules.ruleservice.deployer.RulesDeployerService;
import org.openl.util.StringUtils;

/**
 * Deploys the OpenL Tablets project to an OpenL Tablets repository.
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

        var zipFile = findZipFile(outputDirectory, finalName);

        var server = settings.getServer(deployServer);
        if (server == null) {
            throw new IllegalStateException(
                    "The server configuration with name %s does not exist".formatted(deployServer));
        }

        var properties = new HashMap<String, String>();
        if ("jdbc".equals(deployType)) {
            properties.put("production-repository.factory", "repo-jdbc");
        }
        properties.put("production-repository.uri", deployUrl);
        properties.put("production-repository.login", server.getUsername());
        properties.put("production-repository.password", server.getPassword());
        properties.put("production-repository.base.path", "deploy/");

        try (var deployerService = new RulesDeployerService(properties::get)) {
            deployerService.deploy(zipFile.toPath(), false);
        }
    }

    /**
     * Finds the archive to deploy. The deployment archive of a project that depends on other OpenL projects wins, as it
     * carries these projects too. Otherwise, the only zip file whose name contains the name of the build is deployed,
     * the archive with the tests aside.
     *
     * @throws IllegalStateException when there is no such zip file, or more than one
     */
    static File findZipFile(File outputDirectory, String finalName) {
        var zip = '.' + OpenLPackagings.ZIP_DEPENDENCY_TYPE;
        var deployment = new File(outputDirectory, finalName + '-' + PackageMojo.DEPLOYMENT_CLASSIFIER + zip);
        if (deployment.isFile()) {
            return deployment;
        }
        var tests = finalName + '-' + PackageMojo.TESTS_CLASSIFIER + zip;
        var zipFiles = outputDirectory.listFiles(
                (dir, name) -> name.contains(finalName) && name.endsWith(zip) && !name.equals(tests));
        if (zipFiles == null || zipFiles.length == 0) {
            throw new IllegalStateException("Cannot deploy the rules project, as the zip file does not exist");
        }
        if (zipFiles.length > 1) {
            throw new IllegalStateException("There are more than 1 zip file in the target directory");
        }
        return zipFiles[0];
    }
}

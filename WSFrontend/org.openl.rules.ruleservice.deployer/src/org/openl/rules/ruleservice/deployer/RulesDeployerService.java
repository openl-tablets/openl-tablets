package org.openl.rules.ruleservice.deployer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.folder.FileChangesFromZip;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class allows to deploy a zip-based project to a production repository.
 *
 * @author Vladyslav Pikus
 */
public class RulesDeployerService implements Closeable {

    private static final String DEFAULT_DEPLOYMENT_NAME = "openl_rules_";
    private static final String DEFAULT_AUTHOR_NAME = "OpenL_Deployer";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Repository deployRepo;

    public RulesDeployerService(Repository repository) {
        this.deployRepo = repository;
    }

    /**
     * Initializes repository using target properties
     * @param properties repository settings
     */
    public RulesDeployerService(Properties properties) {
        Map<String, String> params = new HashMap<>();
        params.put("uri", properties.getProperty("production-repository.uri"));
        params.put("login", properties.getProperty("production-repository.login"));
        params.put("password", properties.getProperty("production-repository.password"));
        // AWS S3 specific
        params.put("bucketName",properties.getProperty("production-repository.bucket-name"));
        params.put("regionName",properties.getProperty("production-repository.region-name"));
        params.put("accessKey",properties.getProperty("production-repository.access-key"));
        params.put("secretKey",properties.getProperty("production-repository.secret-key"));
        // Git specific
        params.put("localRepositoryPath",properties.getProperty("production-repository.local-repository-path"));
        params.put("branch",properties.getProperty("production-repository.branch"));
        params.put("tagPrefix",properties.getProperty("production-repository.tag-prefix"));
        // AWS S3 and Git specific
        params.put("listener-timer-period",properties.getProperty("production-repository.listener-timer-period"));

        this.deployRepo = RepositoryInstatiator
                .newRepository(properties.getProperty("production-repository.factory"), params);
    }

    /**
     * Deploys or redeploys target zip input stream
     *
     * @param name original ZIP file name
     * @param in zip input stream
     * @param overridable if deployment was exist before and overridable is false, it will not be deployed, if true, it will be overridden.
     */
    public void deploy(String name, InputStream in, boolean overridable) throws Exception {
        deployInternal(name, in, overridable);
    }

    public void deploy(InputStream in, boolean overridable) throws Exception {
        deployInternal(null, in, overridable);
    }

    private void deployInternal(String originalName, InputStream in, boolean overridable) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtils.copyAndClose(in, outputStream);
        String name = originalName != null ? originalName : DEFAULT_DEPLOYMENT_NAME + System.currentTimeMillis();

        if (outputStream.size() == 0) {
            throw new RuntimeException("Zip file input stream is empty");
        }

        DeploymentDTO deployment = new DeploymentDTO(name,
            new ByteArrayInputStream(outputStream.toByteArray()),
            outputStream.size());

        deployInternal(deployment, overridable);
    }

    private void deployInternal(DeploymentDTO deployment, boolean overridable) throws Exception {
        ZipInputStream zipStream = null;
        try {
            String projectName = deployment.getName();
            String apiVersion = null;

            zipStream = new ZipInputStream(deployment.getInputStream());
            ZipEntry zipEntry;
            boolean doesNotContainZippedFiles = true;
            while ((zipEntry = zipStream.getNextEntry()) != null) {
                InputStream fileStream = new ZippedFileInputStream(zipStream);
                if (!zipEntry.isDirectory()) {
                    String zippedFileName = DeploymentUtils.getFileName(zipEntry.getName());
                    if ("rules.xml".equals(zippedFileName)) {
                        String name = DeploymentUtils.getProjectName(fileStream);
                        if (name != null && !name.isEmpty()) {
                            projectName = name;
                        }
                    } else if ("rules-deploy.xml".equals(zippedFileName)) {
                        apiVersion = DeploymentUtils.getApiVersion(fileStream);
                    }
                    doesNotContainZippedFiles = false;
                }
                fileStream.close();
            }

            if (doesNotContainZippedFiles) {
                throw new RuntimeException("Target zip file doesn't contain entries");
            }

            String deploymentName = projectName;
            if (apiVersion != null && !apiVersion.isEmpty()) {
                deploymentName += DeploymentUtils.API_VERSION_SEPARATOR + apiVersion;
            }

            if (!overridable && isRulesDeployed(deploymentName)) {
                log.info("Module '{}' is skipped for deploy because it has been already deployed.", deploymentName);
                return;
            }

            InputStream inputStream = deployment.getInputStream();
            inputStream.reset();
            doDeploy(DeploymentUtils.createDeploymentName(deploymentName, projectName),
                deployment.getContentSize(),
                inputStream);
        } finally {
            IOUtils.closeQuietly(zipStream);
        }
    }

    private void doDeploy(String name, Integer contentSize, InputStream inputStream) throws IOException {
        FileData dest = new FileData();
        dest.setName(name);
        dest.setAuthor(DEFAULT_AUTHOR_NAME);

        if (deployRepo instanceof FolderRepository) {
            ((FolderRepository) deployRepo).save(dest, new FileChangesFromZip(new ZipInputStream(inputStream), name));
        } else {
            dest.setSize(contentSize);
            deployRepo.save(dest, inputStream);
        }
    }

    private boolean isRulesDeployed(String deploymentName) throws IOException {
        List<FileData> deployments = deployRepo.list(DeploymentUtils.DEPLOY_PATH + deploymentName + "/");
        return !deployments.isEmpty();
    }

    @Override
    public void close() {
        if (deployRepo instanceof Closeable) {
            // Close repo connection after validation
            IOUtils.closeQuietly((Closeable) deployRepo);
        }
    }
}

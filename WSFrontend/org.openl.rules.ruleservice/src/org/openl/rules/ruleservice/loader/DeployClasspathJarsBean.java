package org.openl.rules.ruleservice.loader;

import static org.springframework.core.io.support.ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX;

import static org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME;

import java.io.File;
import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import org.openl.rules.ruleservice.deployer.DeploymentDescriptor;
import org.openl.rules.ruleservice.deployer.RulesDeployerService;
import org.openl.spring.config.ConditionalOnEnable;

@Slf4j
@ConditionalOnEnable({
        "production-repository.factory != repo-jar",
        "ruleservice.datasource.deploy.classpath.jars != false",
        "ruleservice.datasource.deploy.classpath.jars != NEVER"})
@Component
public class DeployClasspathJarsBean {

    private final RulesDeployerService rulesDeployerService;
    private final DeployStrategy deployStrategy;
    private final Thread deployThread;
    private final long retryPeriod;
    PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();


    public DeployClasspathJarsBean(RulesDeployerService rulesDeployerService,
                                   @Value("${ruleservice.datasource.deploy.classpath.jars}") String deployStrategy,
                                   @Value("${ruleservice.datasource.deploy.classpath.retry-period}") long retryPeriod) {
        this.rulesDeployerService = rulesDeployerService;
        this.deployStrategy = DeployStrategy.fromString(deployStrategy);
        this.retryPeriod = retryPeriod;
        this.deployThread = Thread.ofVirtual().name("deploy-classpath-jars").unstarted(this::deployLoop);
    }

    @PostConstruct
    public void start() throws Exception {
        deployThread.start();
    }

    private void processResources(ArrayDeque<File> filesToDeploy, String location) {
        try {
            for (var rulesXmlResource : resourceResolver.getResources(location)) {
                try {
                    var resourceURL = rulesXmlResource.getURL();
                    if ("jar".equals(resourceURL.getProtocol())) {
                        resourceURL = ResourceUtils.extractJarFileURL(resourceURL);
                    }
                    filesToDeploy.add(ResourceUtils.getFile(resourceURL));
                } catch (Exception e) {
                    log.warn("Failed to load a resource.", e);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to search resources.", e);
        }
    }

    private void deployLoop() {
        var filesToDeploy = new ArrayDeque<File>(); // Package level for tests only
        try {
            processResources(filesToDeploy, CLASSPATH_ALL_URL_PREFIX + PROJECT_DESCRIPTOR_FILE_NAME);
            processResources(filesToDeploy, CLASSPATH_ALL_URL_PREFIX + DeploymentDescriptor.XML.getFileName());
            processResources(filesToDeploy, CLASSPATH_ALL_URL_PREFIX + DeploymentDescriptor.YAML.getFileName());
            processResources(filesToDeploy, "/openl/*.zip");

            var ready = false; // The deployment repository ready status

            log.info("Deploying {} jars...", filesToDeploy.size());
            while (!filesToDeploy.isEmpty()) {
                if (Thread.interrupted()) {
                    log.info("Deploy jars task is interrupted.");
                    return;
                }
                if (!ready && !rulesDeployerService.isReady()) {
                    log.info("Rules deployer service is not ready. Wait {} seconds...", retryPeriod);
                    TimeUnit.SECONDS.sleep(retryPeriod);
                    continue;
                } else {
                    ready = true;
                }

                try {
                    // Deploy a file from the queue
                    var file = filesToDeploy.peek();
                    rulesDeployerService.deploy(file, isOverwrite());
                    // File was deployed successfully. Remove it from the queue.
                    filesToDeploy.remove();
                    log.info("File '{}' was deployed successfully.", file);
                } catch (Exception e) {
                    ready = false;
                    log.warn(e.getMessage(), e);
                    TimeUnit.SECONDS.sleep(retryPeriod);
                }
            }
            log.info("All jars were deployed successfully.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Deploy jars task is interrupted.");
        }
    }

    private boolean isOverwrite() {
        return switch (deployStrategy) {
            case IF_ABSENT -> false;
            case ALWAYS -> true;
            case NEVER -> throw new IllegalStateException("'NEVER' deploy strategy should not be here");
        };
    }

    public boolean isDone() {
        return deployThread != null && !deployThread.isAlive();
    }

    @PreDestroy
    public void destroy() throws Exception {
        deployThread.interrupt();
        deployThread.join(TimeUnit.SECONDS.toMillis(retryPeriod * 3));
    }
}

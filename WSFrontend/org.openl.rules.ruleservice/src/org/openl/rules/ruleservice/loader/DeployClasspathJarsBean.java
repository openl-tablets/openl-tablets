package org.openl.rules.ruleservice.loader;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import org.openl.rules.repository.zip.ClasspathArchives;
import org.openl.rules.ruleservice.deployer.RulesDeployerService;
import org.openl.spring.config.ConditionalOnEnable;

/**
 * Deploys the OpenL projects packed into the JAR files of the classpath to the deployment repository.
 *
 * <p>The projects are found as {@link ClasspathArchives} tells, so the projects packed into a Spring Boot executable
 * jar are deployed too. Each project is deployed once.
 *
 * <p>The deployment starts when the application starts. It runs in the background and retries until the
 * deployment repository accepts every project.
 *
 * <p>The bean is created eagerly, even inside a configuration whose beans are lazy by default, so the deployment
 * never waits for another bean to ask for it.
 */
@Slf4j
@Lazy(false)
@ConditionalOnEnable({
        "production-repository.factory != repo-jar",
        "ruleservice.datasource.deploy.classpath.jars != NEVER"})
@Component
public class DeployClasspathJarsBean {

    private final RulesDeployerService rulesDeployerService;
    private final DeployStrategy deployStrategy;
    private final Thread deployThread;
    private final long retryPeriod;
    ClasspathArchives archives = new ClasspathArchives(); // Package level for tests only

    public DeployClasspathJarsBean(RulesDeployerService rulesDeployerService,
                                   @Value("${ruleservice.datasource.deploy.classpath.jars}") DeployStrategy deployStrategy,
                                   @Value("${ruleservice.datasource.deploy.classpath.retry-period}") long retryPeriod) {
        this.rulesDeployerService = rulesDeployerService;
        this.deployStrategy = deployStrategy;
        this.retryPeriod = retryPeriod;
        this.deployThread = Thread.ofVirtual().name("deploy-classpath-jars").unstarted(this::deployLoop);
    }

    @PostConstruct
    public void start() {
        deployThread.start();
    }

    private void deployLoop() {
        try {
            var filesToDeploy = new ArrayDeque<>(archives.archives());

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
                }

                ready = deployFirst(filesToDeploy);
            }
            log.info("The deployment of the jars is finished.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Deploy jars task is interrupted.");
        }
    }

    /**
     * Deploys the first file of the queue and removes it from the queue. When the deployment fails, waits for the retry
     * period and leaves the file in the queue.
     *
     * <p>A file the deployer rejects as invalid, such as an empty archive or no archive at all, is removed from the
     * queue with an error in the log, since a retry cannot deploy it, and the files behind it are deployed.
     *
     * @return {@code false} when the deployment failed
     */
    private boolean deployFirst(ArrayDeque<Path> filesToDeploy) throws InterruptedException {
        var file = filesToDeploy.peek();
        try {
            rulesDeployerService.deploy(file, isOverwrite());
            filesToDeploy.remove();
            log.info("File '{}' was deployed successfully.", file);
            return true;
        } catch (IllegalArgumentException e) {
            filesToDeploy.remove();
            log.error("File '{}' is not deployed: {}", file, e.getMessage(), e);
            return true;
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            TimeUnit.SECONDS.sleep(retryPeriod);
            return false;
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
    public void destroy() throws InterruptedException {
        deployThread.interrupt();
        deployThread.join(TimeUnit.SECONDS.toMillis(retryPeriod * 3));
    }
}

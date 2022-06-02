package org.openl.rules.ruleservice.loader;

import static org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;
import org.openl.rules.ruleservice.deployer.DeploymentDescriptor;
import org.openl.rules.ruleservice.deployer.RulesDeployerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

public class DeployClasspathJarsBean implements InitializingBean, DisposableBean {

    private final Logger log = LoggerFactory.getLogger(DeployClasspathJarsBean.class);

    private boolean enabled = false;
    private boolean ignoreIfExists = false;

    private RulesDeployerService rulesDeployerService;
    private Queue<File> filesToDeploy = new LinkedList<>();
    private ScheduledExecutorService scheduledPool;
    private long retryPeriod = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setRulesDeployerService(RulesDeployerService rulesDeployerService) {
        this.rulesDeployerService = rulesDeployerService;
    }

    public void setIgnoreIfExists(boolean ignoreIfExists) {
        this.ignoreIfExists = ignoreIfExists;
    }

    public void setRetryPeriod(long retryPeriod) {
        this.retryPeriod = retryPeriod;
    }

    /**
     * For tests only. Allows setting files that will be deployed.
     */
    void setFilesToDeploy(Collection<File> filesToDeploy) {
        this.filesToDeploy = new LinkedList<>(filesToDeploy);
    }

    private void deployJarForJboss(URL resourceURL) throws Exception {
        // This reflection implementation for JBoss vfs
        String urlString = resourceURL.toString();
        urlString = urlString.substring(0, urlString.lastIndexOf(".jar") + 4);
        Object jarFile = new URL(urlString).openConnection().getContent();
        Class<?> clazz = jarFile.getClass();
        if ("org.jboss.vfs.VirtualFile".equals(clazz.getName())) {
            Method getNameMethod = clazz.getMethod("getName");
            String jarName = (String) getNameMethod.invoke(jarFile);

            Method getPhysicalFileMethod = clazz.getMethod("getPhysicalFile");
            File contentsFile = (File) getPhysicalFileMethod.invoke(jarFile);
            File dir = contentsFile.getParentFile();
            File physicalFile = new File(dir, jarName);

            filesToDeploy.add(physicalFile);
        } else {
            throw new RuleServiceRuntimeException(
                "Protocol VFS supports only for JBoss VFS. URL content must be org.jboss.vfs.VirtualFile.");
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!isEnabled()) {
            return;
        }

        PathMatchingResourcePatternResolver prpr = new PathMatchingResourcePatternResolver();
        processResources(prpr.getResources(createClasspathPattern(PROJECT_DESCRIPTOR_FILE_NAME)));
        processResources(prpr.getResources(createClasspathPattern(DeploymentDescriptor.XML.getFileName())));
        processResources(prpr.getResources(createClasspathPattern(DeploymentDescriptor.YAML.getFileName())));

        Resource[] archives;
        try {
            archives = prpr.getResources("/openl/*.zip");
        } catch (FileNotFoundException ignored) {
            archives = null;
        }
        if (archives != null) {
            processResources(archives);
        }

        scheduledPool = Executors.newSingleThreadScheduledExecutor();
        scheduledPool.scheduleWithFixedDelay(this::deployFiles, 0, retryPeriod, TimeUnit.SECONDS);
    }

    private static String createClasspathPattern(String fileName) {
        return ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + fileName;
    }

    private void processResources(Resource[] resources) throws Exception {
        for (Resource rulesXmlResource : resources) {
            File file;
            try {
                final URL resourceURL = rulesXmlResource.getURL();
                if ("jar".equals(resourceURL.getProtocol()) || "wsjar".equals(resourceURL.getProtocol())) {
                    URL jarUrl = org.springframework.util.ResourceUtils.extractJarFileURL(resourceURL);
                    file = org.springframework.util.ResourceUtils.getFile(jarUrl);
                } else if ("vfs".equals(rulesXmlResource.getURL().getProtocol())) {
                    // This reflection implementation for JBoss vfs
                    deployJarForJboss(resourceURL);
                    continue;
                } else if ("file".equals(resourceURL.getProtocol())) {
                    file = org.springframework.util.ResourceUtils.getFile(resourceURL);
                } else {
                    throw new RuleServiceRuntimeException(
                        "Protocol for URL is not supported! URL: " + resourceURL);
                }
            } catch (Exception e) {
                log.error("Failed to load a resource.", e);
                throw new IOException("Failed to load a resource.", e);
            }
            if (!file.exists()) {
                throw new IOException("File is not found. File: " + file.getAbsolutePath());
            }

            filesToDeploy.add(file);
        }
    }

    private void deployFiles() {
        if (filesToDeploy.isEmpty()) {
            scheduledPool.shutdown();
            return;
        }
        if (!rulesDeployerService.isReady()) {
            // Wait until it will be ready.
            log.info("Rules deployer service isn't ready. Wait {} seconds...", retryPeriod);
            return;
        }
        try {
            log.info("Deploying {} jars...", filesToDeploy.size());
            File file = filesToDeploy.peek();
            while (file != null) {
                if (Thread.currentThread().isInterrupted()) {
                    log.info("Deploy jars task is interrupted.");
                    return;
                }

                rulesDeployerService.deploy(file, ignoreIfExists);
                // File was deployed successfully. Remove it from the queue.
                filesToDeploy.remove();

                // Get next file to deploy.
                file = filesToDeploy.peek();
            }

            log.info("All jars were deployed successfully.");
            // We deployed all files. Can shut down the pool.
            scheduledPool.shutdown();
        } catch (Exception e) {
            // Probably connection is lost after rulesDeployerService.isReady() was true.
            // Log error and try to continue on the next invocation.
            log.warn(e.getMessage(), e);
        }
    }

    @Override
    public void destroy() throws Exception {
        if (scheduledPool != null) {
            scheduledPool.shutdown(); // Disable new tasks from being submitted
            try {
                // Wait a while for existing tasks to terminate
                if (!scheduledPool.awaitTermination(retryPeriod * 3, TimeUnit.SECONDS)) {
                    scheduledPool.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!scheduledPool.awaitTermination(retryPeriod * 3, TimeUnit.SECONDS)) {
                        log.warn("Unable to terminate deploy jars task.");
                    }
                }
            } catch (InterruptedException e) {
                // (Re-)Cancel if current thread also interrupted
                scheduledPool.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
    }
}

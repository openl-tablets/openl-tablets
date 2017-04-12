package org.openl.rules.ruleservice.loader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;
import org.openl.rules.workspace.deploy.ProductionRepositoryDeployer;
import org.openl.util.FileUtils;
import org.openl.util.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class DeployClasspathJarsBean implements InitializingBean {

    private final Logger log = LoggerFactory.getLogger(DeployClasspathJarsBean.class);

    private boolean enabled = false;
    private Repository repository;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DeployClasspathJarsBean() {
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    private void deployJarForJboss(URL resourceURL,
            ProductionRepositoryDeployer productionRepositoryDeployer) throws Exception {
        // This reflection implementation for JBoss vfs
        URLConnection conn = resourceURL.openConnection();
        Object content = conn.getContent();
        Class<?> clazz = content.getClass();
        if ("org.jboss.vfs.VirtualFile".equals(clazz.getName())) {
            String urlString = resourceURL.toString();
            urlString = urlString.substring(0, urlString.lastIndexOf(".jar") + 4);
            Object jarFile = new URL(urlString).openConnection().getContent();
            java.lang.reflect.Method getChildrenMethod = clazz.getMethod("getChildren");
            List<?> children = (List<?>) getChildrenMethod.invoke(jarFile);
            if (!children.isEmpty()) {
                Method getNameMethod = clazz.getMethod("getName");
                String name = (String) getNameMethod.invoke(jarFile);
                File tempDir = FileUtils.createTempDirectory();
                try {
                    File newProjectDir = new File(tempDir, FileUtils.getBaseName(name));
                    Class<?> VFSUtilsClazz = Thread.currentThread()
                        .getContextClassLoader()
                        .loadClass("org.jboss.vfs.VFSUtils");
                    java.lang.reflect.Method recursiveCopyMethod = VFSUtilsClazz.getMethod("recursiveCopy",
                        clazz,
                        File.class);
                    newProjectDir.mkdirs();
                    for (Object child : children) {
                        recursiveCopyMethod.invoke(VFSUtilsClazz, child, newProjectDir);
                    }

                    File tmpJarFile = new File(tempDir, name);
                    ZipUtils.archive(newProjectDir, tmpJarFile);
                    productionRepositoryDeployer.deployInternal(tmpJarFile, repository, true);
                } finally {
                    /* Clean up */
                    FileUtils.deleteQuietly(tempDir);
                }
            } else {
                throw new RuleServiceRuntimeException(
                    "Protocol VFS supports only for JBoss VFS. URL content must be org.jboss.vfs.VirtualFile!");
            }
        } else {
            throw new RuleServiceRuntimeException(
                "Protocol VFS supports only for JBoss VFS. URL content must be org.jboss.vfs.VirtualFile!");
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!isEnabled()) {
            return;
        }

        PathMatchingResourcePatternResolver prpr = new PathMatchingResourcePatternResolver();
        Resource[] resources = prpr.getResources(
            PathMatchingResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
        ProductionRepositoryDeployer productionRepositoryDeployer = new ProductionRepositoryDeployer();
        for (Resource rulesXmlResource : resources) {
            File file = null;
            try {
                final URL resourceURL = rulesXmlResource.getURL();
                if ("jar".equals(resourceURL.getProtocol()) || "wsjar".equals(resourceURL.getProtocol())) {
                    URL jarUrl = org.springframework.util.ResourceUtils.extractJarFileURL(resourceURL);
                    file = org.springframework.util.ResourceUtils.getFile(jarUrl);
                } else if ("vfs".equals(rulesXmlResource.getURL().getProtocol())) {
                    // This reflection implementation for JBoss vfs
                    deployJarForJboss(resourceURL, productionRepositoryDeployer);
                    continue;
                } else {
                    throw new RuleServiceRuntimeException(
                        "Protocol for URL isn't supported! URL: " + resourceURL.toString());
                }
            } catch (Exception e) {
                log.error("Invalid resource!", e);
                throw new IOException("Invalid resource", e);
            }
            if (!file.exists()) {
                throw new IOException("File is not found. File: " + file.getAbsolutePath());
            }

            productionRepositoryDeployer.deployInternal(file, repository, true);
        }
    }
}

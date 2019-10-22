package org.openl.rules.ruleservice.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;

import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;
import org.openl.rules.ruleservice.deployer.RulesDeployerService;
import org.openl.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

public class DeployClasspathJarsBean implements InitializingBean {

    private final Logger log = LoggerFactory.getLogger(DeployClasspathJarsBean.class);

    private boolean enabled = false;

    private RulesDeployerService rulesDeployerService;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setRulesDeployerService(RulesDeployerService rulesDeployerService) {
        this.rulesDeployerService = rulesDeployerService;
    }

    private void deployJarForJboss(URL resourceURL) throws Exception {
        // This reflection implementation for JBoss vfs
        URLConnection conn = resourceURL.openConnection();
        Object content = conn.getContent();
        Class<?> clazz = content.getClass();
        if ("org.jboss.vfs.VirtualFile".equals(clazz.getName())) {
            String urlString = resourceURL.toString();
            urlString = urlString.substring(0, urlString.lastIndexOf(".jar") + 4);
            Object jarFile = new URL(urlString).openConnection().getContent();

            Method getNameMethod = clazz.getMethod("getName");
            String jarName = (String) getNameMethod.invoke(jarFile);

            Method getPhysicalFileMethod = clazz.getMethod("getPhysicalFile");
            File contentsFile = (File) getPhysicalFileMethod.invoke(jarFile);
            File dir = contentsFile.getParentFile();
            File physicalFile = new File(dir, jarName);

            rulesDeployerService.deploy(FileUtils.getBaseName(jarName), new FileInputStream(physicalFile), false);
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
        Resource[] resources = prpr.getResources(
            ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
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
                } else {
                    throw new RuleServiceRuntimeException(
                        "Protocol for URL is not supported! URL: " + resourceURL.toString());
                }
            } catch (Exception e) {
                log.error("Failed to load a resource.", e);
                throw new IOException("Failed to load a resource.", e);
            }
            if (!file.exists()) {
                throw new IOException("File is not found. File: " + file.getAbsolutePath());
            }

            rulesDeployerService.deploy(FileUtils.getBaseName(file.getName()), new FileInputStream(file), false);
        }
    }
}

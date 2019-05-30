package org.openl.rules.ruleservice.loader;

import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Bean to unpack jar with rules.xml to defined folder. This bean is used by
 * FileSystemDataSource. Set depend-on property in bean definition. This class
 * implements InitializingBean.
 *
 * @author Marat Kamalov
 */
public class UnpackClasspathJarToDirectoryBean implements InitializingBean {
    private final Logger log = LoggerFactory.getLogger(UnpackClasspathJarToDirectoryBean.class);

    public final static String DEPLOYMENT_DESCRIPTOR_FILE_NAME = "deployment.xml";
    
    private String destinationDirectory;

    private boolean createAndClearDirectory = true;

    private boolean unpackAllJarsInOneDeployment = true;
    private boolean supportDeploymentVersion = false;
    private boolean enabled = true;

    private String deploymentVersionSuffix = "_v0.0.1";

    public boolean isUnpackAllJarsInOneDeployment() {
        return unpackAllJarsInOneDeployment;
    }

    public void setUnpackAllJarsInOneDeployment(boolean unpackAllJarsInOneDeployment) {
        this.unpackAllJarsInOneDeployment = unpackAllJarsInOneDeployment;
    }

    public String getDeploymentVersionSuffix() {
        return deploymentVersionSuffix;
    }

    public void setDeploymentVersionSuffix(String deploymentVersionSuffix) {
        this.deploymentVersionSuffix = deploymentVersionSuffix;
    }

    /**
     * This bean is used by spring context. DestinationDirectory property must
     * be set in spring configuration. Destination directory should be exist.
     */
    public UnpackClasspathJarToDirectoryBean() {
    }

    /**
     * Returns directory to unpack path.
     *
     * @return destinationDirectory
     */
    public String getDestinationDirectory() {
        return destinationDirectory;
    }

    public void setCreateAndClearDirectory(boolean createAndClearDirectory) {
        this.createAndClearDirectory = createAndClearDirectory;
    }

    public boolean isCreateAndClearDirectory() {
        return createAndClearDirectory;
    }

    /**
     * Sets directory to unpack path.
     *
     * @param destinationDirectory
     */
    public void setDestinationDirectory(String destinationDirectory) {
        if (destinationDirectory == null) {
            throw new IllegalArgumentException("destinationDirectory argument can't be null");
        }
        this.destinationDirectory = destinationDirectory;
    }

    public boolean isSupportDeploymentVersion() {
        return supportDeploymentVersion;
    }

    public void setSupportDeploymentVersion(boolean supportDeploymentVersion) {
        this.supportDeploymentVersion = supportDeploymentVersion;
    }

    /*
     * private static String getPathJar(Resource resource) throws
     * IllegalStateException, IOException { URL location = resource.getURL();
     * String jarPath = location.getPath(); if (jarPath.lastIndexOf("!") == -1)
     * { return null; } String path = jarPath.substring("file:".length(),
     * jarPath.lastIndexOf("!"));
     * 
     * // Workaround for WebSphere 8.5 path = path.replaceAll("%20", " ");
     * 
     * return path; }
     */

    private static void unpack(File jarFile, File destDir) throws IOException {
        JarFile jar = null;
        try {
            jar = new JarFile(jarFile);

            Enumeration<JarEntry> e = jar.entries();
            while (e.hasMoreElements()) {
                JarEntry file = e.nextElement();
                File f = new File(destDir, file.getName());
                if (file.isDirectory()) {
                    f.mkdir();
                    continue;
                }

                InputStream is = jar.getInputStream(file);
                InputStream bufferedInputStream = new BufferedInputStream(is);

                FileOutputStream fos = new FileOutputStream(f);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                int data;
                while ((data = bufferedInputStream.read()) != -1) {
                    bos.write(data);
                }
                bos.close();
                bufferedInputStream.close();
            }
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException e) {

                }
            }
        }
    }

    private static boolean checkOrCreateFolder(File location) {
        if (location.exists()) {
            return true;
        } else {
            return location.mkdirs();
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private void extractJarForJboss(URL resourceURL, File desFile, boolean isDeploymentJar) throws IOException,
                                                                  NoSuchMethodException,
                                                                  InvocationTargetException,
                                                                  IllegalAccessException,
                                                                  ClassNotFoundException {
        // This reflection implementation for JBoss vfs
        URLConnection conn = resourceURL.openConnection();
        Object content = conn.getContent();
        String urlString = resourceURL.toString();
        urlString = urlString.substring(0, urlString.lastIndexOf(".jar") + 4);
        Object jarFile = new URL(urlString).openConnection().getContent();
        Class<?> clazz = jarFile.getClass();
        if ("org.jboss.vfs.VirtualFile".equals(clazz.getName())) {
            java.lang.reflect.Method getChildrenMethod = clazz.getMethod("getChildren");
            List<?> children = (List<?>) getChildrenMethod.invoke(jarFile);
            if (!children.isEmpty()) {
                Method getNameMethod = clazz.getMethod("getName");
                String name = (String) getNameMethod.invoke(jarFile);

                File d = desFile;
                if (!isUnpackAllJarsInOneDeployment()) {
                    String folderName = FileUtils.getBaseName(name);
                    if (isSupportDeploymentVersion()) {
                        folderName = folderName + getDeploymentVersionSuffix();
                    }
                    d = new File(desFile, folderName);
                    d.mkdirs();
                }

                File newProjectDir = null;
                if (!isDeploymentJar){
                    newProjectDir = new File(d, FileUtils.getBaseName(name));
                    newProjectDir.mkdirs();
                }else{
                    newProjectDir = d;
                }
                
                Class<?> VFSUtilsClazz = Thread.currentThread()
                    .getContextClassLoader()
                    .loadClass("org.jboss.vfs.VFSUtils");
                java.lang.reflect.Method recursiveCopyMethod = VFSUtilsClazz.getMethod("recursiveCopy",
                    clazz,
                    File.class);

                for (Object child : children) {
                    recursiveCopyMethod.invoke(VFSUtilsClazz, child, newProjectDir);
                }
            } else {
                throw new RuleServiceRuntimeException("Protocol VFS supported only for JBoss VFS. URL content should be org.jboss.vfs.VirtualFile!");
            }
        } else {
            throw new RuleServiceRuntimeException("Protocol VFS supported only for JBoss VFS. URL content should be org.jboss.vfs.VirtualFile!");
        }
    }

    public void afterPropertiesSet() throws IOException {
        if (!isEnabled()){
            return;
        }
        String destDirectory = getDestinationDirectory();
        if (destDirectory == null) {
            throw new IllegalStateException("Distination directory is null. Please, check bean configuration.");
        }

        File desFile = new File(destDirectory);

        if (!isCreateAndClearDirectory()) {
            if (!desFile.exists()) {
                throw new IOException("Destination folder does not exist. Path: " + destDirectory);
            }

            if (!desFile.isDirectory()) {
                throw new IOException("Destination path isn't a directory on file system. Path: " + destDirectory);
            }
        } else {
            if (checkOrCreateFolder(desFile)) {
                log.info("Destination folder is already exist. Path: {}", destDirectory);
            } else {
                log.info("Destination folder was created. Path: {}", destDirectory);
            }
        }

        PathMatchingResourcePatternResolver prpr = new PathMatchingResourcePatternResolver();
        Resource[] resources = prpr.getResources(PathMatchingResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
        if (!FolderHelper.clearFolder(new File(destDirectory))) {
            log.warn("Failed on a folder clear. Path: \"{}\"", destDirectory);
        }
        for (Resource rulesXmlResource : resources) {
            File file = null;
            try {
                final URL resourceURL = rulesXmlResource.getURL();
                if ("jar".equals(resourceURL.getProtocol()) || "wsjar".equals(resourceURL.getProtocol())) {
                    URL jarUrl = org.springframework.util.ResourceUtils.extractJarFileURL(resourceURL);
                    file = org.springframework.util.ResourceUtils.getFile(jarUrl);
                } else if ("vfs".equals(rulesXmlResource.getURL().getProtocol())) {
                    // This reflection implementation for JBoss vfs
                    extractJarForJboss(resourceURL, desFile, false);
                    log.info("Unpacking \"{}\" into \"{}\" was completed", resourceURL, destDirectory);
                    continue;
                } else {
                    throw new RuleServiceRuntimeException("Protocol for URL doesn't supported! URL: " + resourceURL.toString());
                }
            } catch (Exception e) {
                log.error("Invalid resource!", e);
                throw new IOException("Invalid resource", e);
            }
            if (!file.exists()) {
                throw new IOException("File not found. File: " + file.getAbsolutePath());
            }

            File d = desFile;
            if (!isUnpackAllJarsInOneDeployment()) {
                String folderName = FileUtils.getBaseName(file.getCanonicalPath());
                if (isSupportDeploymentVersion()) {
                    folderName = folderName + getDeploymentVersionSuffix();
                }
                d = new File(desFile, folderName);
                d.mkdirs();
            }

            File destDir = new File(d, FileUtils.getBaseName(file.getCanonicalPath()));
            destDir.mkdirs();
            unpack(file, destDir);

            log.info("Unpacking \"{}\" into \"{}\" was completed", file.getAbsolutePath(), destDirectory);
        }
        
        if (!isUnpackAllJarsInOneDeployment()){
            Resource[] deploymentResources = prpr.getResources(PathMatchingResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + DEPLOYMENT_DESCRIPTOR_FILE_NAME);
            for (Resource deploymentResource : deploymentResources) {
                File file = null;
                try {
                    final URL resourceURL = deploymentResource.getURL();
                    if ("jar".equals(resourceURL.getProtocol()) || "wsjar".equals(resourceURL.getProtocol())) {
                        URL jarUrl = org.springframework.util.ResourceUtils.extractJarFileURL(resourceURL);
                        file = org.springframework.util.ResourceUtils.getFile(jarUrl);
                    } else if ("vfs".equals(deploymentResource.getURL().getProtocol())) {
                        // This reflection implementation for JBoss vfs
                        extractJarForJboss(resourceURL, desFile, true);
                        log.info("Unpacking \"{}\" into \"{}\" was completed", resourceURL, destDirectory);
                        continue;
                    } else {
                        throw new RuleServiceRuntimeException("Protocol for URL doesn't supported! URL: " + resourceURL.toString());
                    }
                } catch (Exception e) {
                    log.error("Invalid resource!", e);
                    throw new IOException("Invalid resource", e);
                }
                if (!file.exists()) {
                    throw new IOException("File not found. File: " + file.getAbsolutePath());
                }
    
                String folderName = FileUtils.getBaseName(file.getCanonicalPath());
                if (isSupportDeploymentVersion()) {
                    folderName = folderName + getDeploymentVersionSuffix();
                }
                
                File d = new File(desFile, folderName);
                d.mkdirs();
    
                unpack(file, d);
    
                log.info("Unpacking \"{}\" into \"{}\" was completed", file.getAbsolutePath(), destDirectory);
            }
        }
    }
}

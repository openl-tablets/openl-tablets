package org.openl.rules.ruleservice.loader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Bean to unpack jar with rules.xml to defined folder. This bean is used by
 * FileSystemDataSource. Set depend-on property in bean definition. This class
 * implements InitializingBean.
 * 
 * @author Marat Kamalov
 * 
 */
public class UnpackClasspathJarToDirectoryBean implements InitializingBean {
    private final Log log = LogFactory.getLog(UnpackClasspathJarToDirectoryBean.class);

    private final static String RULES_FILE_NAME = "rules.xml";

    private String destinationDirectory;

    private boolean createAndClearDirectory = true;

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

    private static String getPathJar(Resource resource) throws IllegalStateException, IOException {
        URL location = resource.getURL();
        String jarPath = location.getPath();
        if (jarPath.lastIndexOf("!") == -1) {
            return null;
        }
        String path = jarPath.substring("file:".length(), jarPath.lastIndexOf("!"));

        // Workaround for WebSphere 8.5
        path = path.replaceAll("%20", " ");

        return path;
    }

    private static void unpack(File jarFile, String destDir) throws IOException {
        File newProjectDir = new File(destDir, FilenameUtils.getBaseName(jarFile.getCanonicalPath()));
        newProjectDir.mkdirs();

        JarFile jar = null;
        try {
            jar = new JarFile(jarFile);

            Enumeration<JarEntry> e = jar.entries();
            while (e.hasMoreElements()) {
                JarEntry file = e.nextElement();
                File f = new File(newProjectDir, file.getName());
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

    public void afterPropertiesSet() throws IOException {
        if (getDestinationDirectory() == null) {
            throw new IllegalStateException("Distination directory is null. Please, check bean configuration.");
        }

        File desFile = new File(getDestinationDirectory());

        if (!isCreateAndClearDirectory()) {
            if (!desFile.exists()) {
                throw new IOException("Destination folder does not exist. Path: " + getDestinationDirectory());
            }

            if (!desFile.isDirectory()) {
                throw new IOException("Destination path isn't a directory on file system. Path: " + getDestinationDirectory());
            }
        } else {
            if (checkOrCreateFolder(desFile)) {
                if (log.isInfoEnabled()) {
                    log.info("Destination folder is already exist. Path: " + getDestinationDirectory());
                }
            } else {
                if (log.isInfoEnabled()) {
                    log.info("Destination folder was created. Path: " + getDestinationDirectory());
                }
            }
        }

        PathMatchingResourcePatternResolver prpr = new PathMatchingResourcePatternResolver();
        Resource[] resources = prpr.getResources(PathMatchingResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + RULES_FILE_NAME);
        if (!FolderHelper.clearFolder(new File(getDestinationDirectory()))) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Failed on a folder clear. Path: \"%s\"", getDestinationDirectory()));
            }
        }
        for (Resource rulesXmlResource : resources) {
            String path = getPathJar(rulesXmlResource);
            if (path != null) {
                File file = new File(path);

                if (!file.exists()) {
                    throw new IOException("File not found. File: " + path);
                }

                unpack(file, getDestinationDirectory());

                if (log.isInfoEnabled()) {
                    log.info(String.format("Unpacking \"" + file.getAbsolutePath() + "\" into \"%s\" was completed",
                        getDestinationDirectory()));
                }
            }
        }
    }
}

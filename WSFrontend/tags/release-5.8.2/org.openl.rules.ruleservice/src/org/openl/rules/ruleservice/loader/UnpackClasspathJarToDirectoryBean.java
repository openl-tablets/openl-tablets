package org.openl.rules.ruleservice.loader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FilenameUtils;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.util.Log;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Bean to unpack jar with rules.xml to defined folder. This bean is used by
 * FileSystemDataSource. Set depend-on property in bean definition. This class
 * implements InitializingBean.
 * 
 * @author MKamalov
 * 
 */
public class UnpackClasspathJarToDirectoryBean implements InitializingBean {
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
     * Returns directory to unpack path
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
     * Sets directory to unpack path
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
        return jarPath.substring("file:".length(), jarPath.lastIndexOf("!"));
    }

    private static void unpack(File jarFile, String destDir) throws IOException {
        File newProjectDir = new File(destDir, FilenameUtils.getBaseName(jarFile.getCanonicalPath()));
        newProjectDir.mkdirs();

        JarFile jar = new JarFile(jarFile);

        Enumeration<JarEntry> e = jar.entries();
        while (e.hasMoreElements()) {
            JarEntry file = e.nextElement();
            File f = new File(newProjectDir, file.getName());
            if (file.isDirectory()) {
                f.mkdir();
                continue;
            }
            InputStream is = jar.getInputStream(file);

            FileOutputStream fos = new FileOutputStream(f);
            while (is.available() > 0) {
                fos.write(is.read());
            }
            fos.close();
            is.close();
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
            throw new IllegalStateException("Distination directory is null. Check bean configuration.");
        }

        File desFile = new File(getDestinationDirectory());

        if (!isCreateAndClearDirectory()) {
            if (!desFile.exists()) {
                throw new IOException("Destination folder does not exist. Path: " + getDestinationDirectory());
            }

            if (!desFile.isDirectory()) {
                throw new IOException("Destination path isn't a directory on file system. Path: "
                        + getDestinationDirectory());
            }
        } else {
            if (checkOrCreateFolder(desFile)) {
                if (Log.isInfoEnabled()) {
                    Log.info("Destination folder is already exist");
                }
            } else {
                if (Log.isInfoEnabled()) {
                    Log.info("Destination folder was created");
                }
            }
        }

        PathMatchingResourcePatternResolver prpr = new PathMatchingResourcePatternResolver();
        Resource[] resources = prpr.getResources(PathMatchingResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + RULES_FILE_NAME);
        FolderHelper.clearFolder(new File(getDestinationDirectory()));
        for (Resource rulesXmlResource : resources) {
            String path = getPathJar(rulesXmlResource);
            File file = new File(path);

            if (!file.exists()) {
                throw new IOException("File not found. File path: " + path);
            }

            unpack(file, getDestinationDirectory());
        }
    }
}

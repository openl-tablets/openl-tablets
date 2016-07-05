package org.openl.rules.project.xml;

import java.io.*;
import java.util.Properties;

import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SupportedVersionSerializer {
    private static final String OPENL_PROJECT_PROPERTIES_FILE = "openl-project.properties";
    private static final String OPENL_COMPATIBILITY_VERSION = "openl.compatibility.version";
    private final Logger log = LoggerFactory.getLogger(SupportedVersionSerializer.class);
    private final SupportedVersion defaultVersion;

    public SupportedVersionSerializer(String defaultVersion) {
        this.defaultVersion = StringUtils.isBlank(defaultVersion) ?
                         SupportedVersion.getLastVersion() :
                         SupportedVersion.getByVersion(defaultVersion);
    }

    public SupportedVersion getSupportedVersion(File projectFolder) {
        SupportedVersion version = null;

        File folder = new File(projectFolder, FolderHelper.PROPERTIES_FOLDER);
        File file = new File(folder, OPENL_PROJECT_PROPERTIES_FILE);

        if (folder.isDirectory() && file.isFile()) {
            Properties properties = new Properties();
            InputStream is = null;
            try {
                is = new FileInputStream(file);
                properties.load(is);
                String compatibility = properties.getProperty(OPENL_COMPATIBILITY_VERSION);
                if (compatibility != null) {
                    version = SupportedVersion.getByVersion(compatibility);
                }
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error(e.getMessage(), e);
                }
            } finally {
                IOUtils.closeQuietly(is);
            }
        }

        return version == null ? defaultVersion : version;
    }

    public void setSupportedVersion(File projectFolder, SupportedVersion version) throws IOException {

        Properties properties = new Properties();
        properties.setProperty(OPENL_COMPATIBILITY_VERSION, version.getVersion());

        FileOutputStream os = null;
        try {
            File folder = new File(projectFolder, FolderHelper.PROPERTIES_FOLDER);
            if (!folder.exists()) {
                if (!folder.mkdir() && !folder.isDirectory()) {
                    throw new IOException("Can't create folder " + folder);
                }
            }
            File file = new File(folder, OPENL_PROJECT_PROPERTIES_FILE);
            os = new FileOutputStream(file);
            properties.store(os, "Openl project properties");
            os.close();
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    public SupportedVersion getDefaultVersion() {
        return defaultVersion;
    }
}
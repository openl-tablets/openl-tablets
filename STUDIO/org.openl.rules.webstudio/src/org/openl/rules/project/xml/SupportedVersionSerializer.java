package org.openl.rules.project.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SupportedVersionSerializer {
    // Filename ".setting" is used instead of ".settings" because the latter is a reserved folder name in old deprecated
    // OpenL projects.
    private static final String OPENL_PROJECT_PROPERTIES_FILE = ".setting";
    private static final String OPENL_COMPATIBILITY_VERSION = "openl.compatibility.version";
    private final Logger log = LoggerFactory.getLogger(SupportedVersionSerializer.class);
    private final SupportedVersion defaultVersion;

    public SupportedVersionSerializer(String defaultVersion) {
        this.defaultVersion = StringUtils.isBlank(defaultVersion) ? SupportedVersion.getLastVersion()
                                                                  : SupportedVersion.getByVersion(defaultVersion);
    }

    public SupportedVersion getSupportedVersion(File projectFolder) {
        SupportedVersion version = null;

        File file = new File(projectFolder, OPENL_PROJECT_PROPERTIES_FILE);

        if (projectFolder.isDirectory() && file.isFile()) {
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

        File file = new File(projectFolder, OPENL_PROJECT_PROPERTIES_FILE);
        if (version.isLastVersion()) {
            // Currently, the file contains only one property, so we can safely delete the file instead of removing the
            // single property from it.
            if (!file.exists() || file.delete()) {
                return;
            }
            // If the file exists and can't be deleted, proceed with version update.
        }

        Properties properties = new Properties();
        properties.setProperty(OPENL_COMPATIBILITY_VERSION, version.getVersion());

        FileOutputStream os = null;
        try {
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
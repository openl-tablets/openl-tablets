package org.openl.rules.project.xml;

import java.io.*;
import java.util.Properties;

import org.apache.poi.util.IOUtils;
import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.xml.v5_11.XmlProjectDescriptorSerializer_v5_11;
import org.openl.rules.project.xml.v5_12.XmlProjectDescriptorSerializer_v5_12;
import org.openl.rules.project.xml.v5_13.XmlProjectDescriptorSerializer_v5_13;
import org.openl.rules.project.xml.v5_16.XmlProjectDescriptorSerializer_v5_16;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectDescriptorSerializerFactory {
    private static final String OPENL_PROJECT_PROPERTIES_FILE = "openl-project.properties";
    private static final String OPENL_COMPATIBILITY_VERSION = "openl.compatibility.version";
    private final Logger log = LoggerFactory.getLogger(ProjectDescriptorSerializerFactory.class);

    private final SupportedVersion defaultVersion;

    public ProjectDescriptorSerializerFactory(String defaultVersion) {
        this.defaultVersion = StringUtils.isBlank(defaultVersion) ?
                              SupportedVersion.getLastVersion() :
                              SupportedVersion.getByVersion(defaultVersion);
    }

    public IProjectDescriptorSerializer getDefaultSerializer() {
        return getSerializer(defaultVersion);
    }

    public IProjectDescriptorSerializer getSerializer(File projectFolder) {
        return getSerializer(getSupportedVersion(projectFolder));
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

    public IProjectDescriptorSerializer getSerializer(SupportedVersion version) {
        switch (version) {
            case V5_11:
                return new XmlProjectDescriptorSerializer_v5_11();
            case V5_12:
                return new XmlProjectDescriptorSerializer_v5_12();
            case V5_13:
                return new XmlProjectDescriptorSerializer_v5_13();
            case V5_16:
                return new XmlProjectDescriptorSerializer_v5_16();
            default:
                throw new UnsupportedOperationException("Unsupported OpenL version " + version.getVersion());
        }
    }
}

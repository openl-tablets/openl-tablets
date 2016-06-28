package org.openl.rules.project.xml;

import java.io.*;

import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.xml.v5_11.XmlProjectDescriptorSerializer_v5_11;
import org.openl.rules.project.xml.v5_12.XmlProjectDescriptorSerializer_v5_12;
import org.openl.rules.project.xml.v5_13.XmlProjectDescriptorSerializer_v5_13;
import org.openl.rules.project.xml.v5_16.XmlProjectDescriptorSerializer_v5_16;
import org.openl.util.StringUtils;

public class ProjectDescriptorSerializerFactory {
    private final SupportedVersionSerializer supportedVersionSerializer;

    public ProjectDescriptorSerializerFactory(String defaultVersion) {
        this.supportedVersionSerializer = new SupportedVersionSerializer(defaultVersion);
    }

    public IProjectDescriptorSerializer getDefaultSerializer() {
        return getSerializer(supportedVersionSerializer.getDefaultVersion());
    }

    public IProjectDescriptorSerializer getSerializer(File projectFolder) {
        return getSerializer(supportedVersionSerializer.getSupportedVersion(projectFolder));
    }

    public SupportedVersion getSupportedVersion(File projectFolder) {
        return supportedVersionSerializer.getSupportedVersion(projectFolder);
    }

    public void setSupportedVersion(File projectFolder, SupportedVersion version) throws IOException {
        supportedVersionSerializer.setSupportedVersion(projectFolder, version);
    }

    public IProjectDescriptorSerializer getSerializer(SupportedVersion version) {
        switch (version) {
            case V5_11:
                return new XmlProjectDescriptorSerializer_v5_11();
            case V5_12:
                return new XmlProjectDescriptorSerializer_v5_12();
            case V5_13:
            case V5_14:
            case V5_15:
                return new XmlProjectDescriptorSerializer_v5_13();
            case V5_16:
            default: // rules.xml isn't changed in newer versions of OpenL but rules-deploy.xml could
                return new XmlProjectDescriptorSerializer_v5_16();
        }
    }
}

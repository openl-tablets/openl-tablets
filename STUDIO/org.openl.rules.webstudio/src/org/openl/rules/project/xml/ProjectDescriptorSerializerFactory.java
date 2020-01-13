package org.openl.rules.project.xml;

import java.io.File;
import java.io.IOException;

import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.xml.v5_11.XmlProjectDescriptorSerializer_v5_11;
import org.openl.rules.project.xml.v5_12.XmlProjectDescriptorSerializer_v5_12;
import org.openl.rules.project.xml.v5_13.XmlProjectDescriptorSerializer_v5_13;
import org.openl.rules.project.xml.v5_16.XmlProjectDescriptorSerializer_v5_16;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.file.FileSystemRepository;
import org.springframework.core.env.PropertyResolver;

public class ProjectDescriptorSerializerFactory {
    private final SupportedVersionSerializer supportedVersionSerializer;

    public ProjectDescriptorSerializerFactory(PropertyResolver propertyResolver) {
        this.supportedVersionSerializer = new SupportedVersionSerializer(
            propertyResolver.getProperty("default.openl.compatibility.version"));
    }

    public IProjectDescriptorSerializer getDefaultSerializer() {
        return getSerializer(supportedVersionSerializer.getDefaultVersion());
    }

    public IProjectDescriptorSerializer getSerializer(File projectFolder) {
        return getSerializer(getSupportedVersion(projectFolder));
    }

    /**
     * Get Project Descriptor serializer by any artefact
     *
     * @param artefact can be AProject instance or any resource inside it
     * @return Project Descriptor serializer for supporting OpenL version
     */
    public IProjectDescriptorSerializer getSerializer(AProjectArtefact artefact) {
        AProject project = artefact.getProject();
        Repository repository = project.getRepository();
        if (repository instanceof FileSystemRepository) {
            File root = ((FileSystemRepository) repository).getRoot();
            return getSerializer(new File(root, project.getFolderPath()));
        } else {
            return getDefaultSerializer();
        }
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
            case V5_17:
            case V5_18:
            case V5_19:
            case V5_20:
            case V5_21:
            case V5_22:
            case V5_23:
            default: // rules.xml is not changed in newer versions of OpenL but rules-deploy.xml could
                return new XmlProjectDescriptorSerializer_v5_16();
        }
    }
}

package org.openl.rules.project.xml;

import java.io.File;
import java.io.IOException;

import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.file.FileSystemRepository;

public class ProjectDescriptorSerializerFactory {
    private final SupportedVersionSerializer supportedVersionSerializer;

    public ProjectDescriptorSerializerFactory(String defaultVersion) {
        this.supportedVersionSerializer = new SupportedVersionSerializer(defaultVersion);
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
            case V5_23:
            case V5_24:
            case V5_25:
            case V5_26:
            case V5_27:
            default: // rules.xml is not changed in newer versions of OpenL but rules-deploy.xml could
                return new XmlProjectDescriptorSerializer();
        }
    }
}

package org.openl.rules.webstudio.web;

import static org.openl.rules.project.abstraction.ProjectTags.TAGS_FILE_NAME;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.ResourceTransformer;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.rules.repository.api.FileItem;
import org.openl.util.IOUtils;
import org.openl.util.PropertiesUtils;
import org.openl.util.StringUtils;

public class CopyProjectTransformer implements ResourceTransformer {
    private final Logger log = LoggerFactory.getLogger(CopyProjectTransformer.class);
    private final String newProjectName;
    private final Map<String, String> tags;

    public CopyProjectTransformer(String newProjectName, Map<String, String> tags) {
        this.newProjectName = newProjectName;
        this.tags = tags;
    }

    @Override
    public InputStream transform(AProjectResource resource) throws ProjectException {
        if (isProjectDescriptor(resource)) {
            // Read the stream to memory and try to parse it and then change project name. If it cannot be parsed return
            // original rules.xml.
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                IOUtils.copyAndClose(resource.getContent(), outputStream);
            } catch (IOException e) {
                throw new ProjectException(e.getMessage(), e);
            }
            ByteArrayInputStream copy = new ByteArrayInputStream(outputStream.toByteArray());

            try {
                IProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer();
                ProjectDescriptor projectDescriptor = serializer.deserialize(copy);
                projectDescriptor.setName(newProjectName);
                return IOUtils.toInputStream(serializer.serialize(projectDescriptor));
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
                copy.reset();
                return copy;
            }
        }

        return resource.getContent();
    }

    private boolean isProjectDescriptor(AProjectResource resource) {
        return ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME.equals(resource.getInternalPath());
    }

    @Override
    public List<FileItem> transformChangedFiles(String rootPath, List<FileItem> changes) {
        Optional<FileItem> tagsFile = changes.stream().filter(fileItem -> fileItem.getData().getName().equals(TAGS_FILE_NAME)).findFirst();
        if (!tags.isEmpty() || tagsFile.isPresent()) {
            List<FileItem> changesWithTags = new ArrayList<>(changes);
            tagsFile.ifPresent(file -> IOUtils.closeQuietly(file.getStream()));
            tagsFile.ifPresent(changesWithTags::remove);
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                PropertiesUtils.store(byteArrayOutputStream, tags.entrySet());
                var inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                StringBuilder fullName = new StringBuilder();
                if (StringUtils.isNotBlank(rootPath)) {
                    fullName.append(rootPath);
                    if (! rootPath.endsWith("/")) {
                        fullName.append("/");
                    }
                }
                fullName.append(TAGS_FILE_NAME);
                FileItem newTagFile = new FileItem(fullName.toString(), inputStream);
                changesWithTags.add(newTagFile);
                return changesWithTags;
            } catch (IOException e) {
                log.warn(e.getMessage(), e);
                return changes;
            }
        } else {
            return changes;
        }
    }
}

package org.openl.rules.project.resolving;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.rules.repository.api.FileData;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves specified OpenL project revision's dependencies.
 */
public class ProjectDescriptorArtefactResolver {
    private final Logger log = LoggerFactory.getLogger(ProjectDescriptorArtefactResolver.class);
    private final IProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer();

    /**
     * Project descriptors cache. Replace with ehcache if GC occurs too often.
     */
    private final Map<String, ProjectDescriptor> cache = new WeakHashMap<>();

    private ProjectDescriptor getProjectDescriptor(AProject project) throws ProjectException {
        FileData fileData = project.getFileData();
        if (fileData == null) {
            return null;
        }
        String version = fileData.getVersion();
        String versionName = version == null ? "" : version;
        String key = String.format("%s:%s:%b", project.getName(), versionName, project.isModified());

        ProjectDescriptor descriptor = cache.get(key);
        if (descriptor != null) {
            return descriptor;
        }

        if (!project.hasArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME)) {
            // For performance reasons assume that if there is no rules.xml then there are no project dependencies and
            // project name is got from the project folder name.
            return null;
        }

        AProjectArtefact artefact = project
            .getArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
        if (artefact instanceof AProjectResource) {
            InputStream content = null;
            try {
                content = ((AProjectResource) artefact).getContent();
                descriptor = serializer.deserialize(content);
            } finally {
                IOUtils.closeQuietly(content);
            }
            cache.put(key, descriptor);
            return descriptor;
        }

        return null;
    }

    public List<ProjectDependencyDescriptor> getDependencies(AProject project) throws ProjectException {
        ProjectDescriptor pd = getProjectDescriptor(project);
        return pd != null ? pd.getDependencies() : null;
    }

    public String getLogicalName(AProject project) {
        ProjectDescriptor pd = null;
        try {
            pd = getProjectDescriptor(project);
        } catch (Exception e) {
            // Error in user data, not application logic - debug log level will be used
            log.warn("Cannot get project descriptor for project '{}'. Physical project name will be used. Cause: {}",
                project.getName(),
                e.getMessage(),
                e);
        }
        return pd != null ? pd.getName() : project.getName();
    }

    public void deleteRevisionsFromCache(AProject project) {
        for (String key : new HashSet<>(cache.keySet())) {
            if (key.split(":")[0].equals(project.getName())) {
                cache.remove(key);
            }
        }
    }
}

package org.openl.rules.webstudio.web.repository;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.api.FileData;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

/**
 * Resolves specified OpenL project revision's dependencies.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectDescriptorArtefactResolver {

    static final String CACHE_NAME = "projectDescriptors";

    private final CacheManager cacheManager;

    private ProjectDescriptor getProjectDescriptor(AProject project) throws ProjectException {
        FileData fileData = project.getFileData();
        if (fileData == null) {
            return null;
        }
        // A committed revision's rules.xml is immutable and identical for every user, so cache it by repository,
        // project and revision. The repository id keeps same-named projects in different repositories apart. A
        // modified working copy is read fresh: its rules.xml can change in place without a new revision and must
        // not be shared between users.
        String cacheKey = fileData.getVersion() != null && !project.isModified()
                ? project.getRepository().getId() + '/' + project.getName() + '@' + fileData.getVersion()
                : null;
        var cache = cacheKey == null ? null : cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            ProjectDescriptor cached = cache.get(cacheKey, ProjectDescriptor.class);
            if (cached != null) {
                return cached;
            }
        }

        if (!project.hasArtefact(ProjectDescriptor.FILE_NAME)) {
            // For performance reasons assume that if there is no rules.xml then there are no project
            // dependencies and the project name is taken from the project folder name.
            return null;
        }

        AProjectArtefact artefact = project.getArtefact(ProjectDescriptor.FILE_NAME);
        if (artefact instanceof AProjectResource resource) {
            ProjectDescriptor descriptor;
            InputStream content = null;
            try {
                content = resource.getContent();
                descriptor = ProjectDescriptor.read(content);
            } finally {
                IOUtils.closeQuietly(content);
            }
            if (cache != null) {
                cache.put(cacheKey, descriptor);
            }
            return descriptor;
        }

        return null;
    }

    public List<ProjectDependencyDescriptor> getDependencies(AProject project) throws ProjectException {
        ProjectDescriptor pd = getProjectDescriptor(project);
        return (pd != null && pd.getDependencies() != null) ? pd.getDependencies() : Collections.emptyList();
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
        if (pd != null && StringUtils.isNotBlank(pd.getName())) {
            return pd.getName();
        }
        // rules.xml is absent or does not declare a name - fall back to the physical project folder name
        String actualPath = project.getRealPath();
        return actualPath.substring(actualPath.lastIndexOf('/') + 1);
    }
}

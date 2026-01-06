package org.openl.studio.projects.service.resources;

import java.util.List;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.projects.model.resources.Resource;

/**
 * Service for retrieving project resources (files and folders).
 *
 */
public interface ProjectResourcesService {

    /**
     * Get resources from a project.
     *
     * @param project   the rules project to get resources from
     * @param query     filtering criteria (can be null for no filtering)
     * @param recursive whether to include nested resources recursively
     * @param viewMode  FLAT returns a flat list, NESTED returns tree structure
     * @return list of resources matching the criteria
     */
    List<Resource> getResources(RulesProject project,
                                ResourceCriteriaQuery query,
                                boolean recursive,
                                ResourceViewMode viewMode);
}

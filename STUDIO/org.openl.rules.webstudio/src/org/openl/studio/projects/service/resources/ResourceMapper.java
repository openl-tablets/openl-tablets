package org.openl.studio.projects.service.resources;

import java.util.Comparator;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.studio.projects.model.resources.FileResource;
import org.openl.studio.projects.model.resources.Resource;

/**
 * Maps project artefacts to Resource DTOs.
 */
public interface ResourceMapper {

    Comparator<Resource> RESOURCE_COMPARATOR = Comparator
            .comparing((Resource r) -> r instanceof FileResource)
            .thenComparing(Resource::getName, String.CASE_INSENSITIVE_ORDER);

    Resource map(AProjectArtefact artefact);
}

package org.openl.studio.projects.service;

import jakarta.annotation.Nonnull;

import org.openl.rules.project.abstraction.AProject;
import org.openl.studio.projects.model.ProjectIdModel;

public interface ProjectIdentifierMapper {

    @Nonnull
    ProjectIdModel map(@Nonnull AProject project);

}

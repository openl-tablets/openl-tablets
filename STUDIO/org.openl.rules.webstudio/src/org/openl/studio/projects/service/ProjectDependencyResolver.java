package org.openl.studio.projects.service;

import java.util.List;
import jakarta.xml.bind.JAXBException;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;

/**
 * @author ybiruk
 */
public interface ProjectDependencyResolver {


    /**
     * Returns all projects that project depends on.
     */
    List<RulesProject> getProjectDependencies(RulesProject project);

    /**
     * Returns all projects that depend on project
     */
    List<RulesProject> getDependsOnProject(RulesProject project) throws ProjectException, JAXBException;
}

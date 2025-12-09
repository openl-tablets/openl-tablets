package org.openl.studio.projects.converter;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.ProjectIdModel;

@Component
@ParametersAreNonnullByDefault
public class AProjectConverter implements Converter<String, AProject> {

    private final DesignTimeRepository designTimeRepository;

    public AProjectConverter(DesignTimeRepository designTimeRepository) {
        this.designTimeRepository = designTimeRepository;
    }

    @Override
    public AProject convert(String source) {
        try {
            var projectId = ProjectIdModel.decode(source);
            return designTimeRepository.getProject(projectId.getRepository(), projectId.getProjectName());
        } catch (ProjectException | IllegalArgumentException e) {
            var ex = new NotFoundException("project.identifier.message");
            ex.initCause(e);
            throw ex;
        }
    }
}

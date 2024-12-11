package org.openl.rules.rest.resolver;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.rest.model.ProjectIdModel;
import org.openl.rules.workspace.dtr.DesignTimeRepository;

@Component
@ParametersAreNonnullByDefault
public class AProjectConverter implements Converter<String, AProject> {

    private final DesignTimeRepository designTimeRepository;

    public AProjectConverter(DesignTimeRepository designTimeRepository) {
        this.designTimeRepository = designTimeRepository;
    }

    @Override
    public AProject convert(String source) {
        var projectId = ProjectIdModel.decode(source);
        try {
            return designTimeRepository.getProject(projectId.getRepository(), projectId.getProjectName());
        } catch (ProjectException e) {
            var ex = new NotFoundException("project.identifier.message");
            ex.initCause(e);
            throw ex;
        }
    }
}

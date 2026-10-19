package org.openl.studio.projects.converter;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.workspace.ProjectId;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.common.exception.NotFoundException;

/**
 * Resolves the design project an id names, as its home branch shows it.
 */
@Component
@ParametersAreNonnullByDefault
@RequiredArgsConstructor
public class AProjectConverter implements Converter<String, AProject> {

    private final DesignTimeRepository designTimeRepository;

    @Override
    public AProject convert(String source) {
        return designTimeRepository.getProjectById(ProjectId.of(source))
                .orElseThrow(() -> new NotFoundException("project.identifier.message"));
    }
}

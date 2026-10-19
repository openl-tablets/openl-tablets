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
 *
 * <p>A project whose folder moved is resolved under the id it has now, and a deleted project answers
 * {@code 410 Gone}, while the server runs.
 */
@Component
@ParametersAreNonnullByDefault
@RequiredArgsConstructor
public class AProjectConverter implements Converter<String, AProject> {

    private final DesignTimeRepository designTimeRepository;

    @Override
    public AProject convert(String source) {
        var id = ProjectId.of(source);
        return designTimeRepository.getProjectById(id)
                .or(() -> StaleProjects.find(designTimeRepository, id, designTimeRepository::getProjectById))
                .orElseThrow(() -> new NotFoundException("project.identifier.message"));
    }
}

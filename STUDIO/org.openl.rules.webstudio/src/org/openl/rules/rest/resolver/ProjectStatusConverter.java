package org.openl.rules.rest.resolver;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.ProjectStatus;

/**
 * Custom converter for {@link ProjectStatus} enum.
 *
 * @author Vladyslav Pikus
 */
@Component
@ParametersAreNonnullByDefault
public class ProjectStatusConverter implements Converter<String, ProjectStatus> {

    @Override
    public ProjectStatus convert(String source) {
        if (source.isEmpty()) {
            return null;
        }
        if ("OPENED".equals(source)) {
            return ProjectStatus.VIEWING;
        }

        return ProjectStatus.valueOf(source);
    }
}

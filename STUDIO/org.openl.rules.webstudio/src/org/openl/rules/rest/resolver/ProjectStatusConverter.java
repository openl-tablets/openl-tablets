/* Copyright © 2023 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package org.openl.rules.rest.resolver;

import javax.annotation.ParametersAreNonnullByDefault;

import org.openl.rules.project.abstraction.ProjectStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

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

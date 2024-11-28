package org.openl.rules.rest.model.converters;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.openl.rules.project.abstraction.ProjectStatus;

/**
 * Custom deserializer for {@link ProjectStatus} enum.
 *
 * @author Vladyslav Pikus
 */
public class ProjectStatusDeserializer extends JsonDeserializer<ProjectStatus> {

    @Override
    public ProjectStatus deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
        String value = parser.getValueAsString();

        if (value == null) {
            return null;
        }

        if ("OPENED".equals(value)) {
            return ProjectStatus.VIEWING;
        }

        return ProjectStatus.valueOf(value);
    }
}

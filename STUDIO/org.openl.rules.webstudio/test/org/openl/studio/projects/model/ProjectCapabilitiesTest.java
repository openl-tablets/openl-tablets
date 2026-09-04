package org.openl.studio.projects.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.openl.studio.common.model.Capabilities;

class ProjectCapabilitiesTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializesProjectCapabilitiesWithoutWrapperName() throws JsonProcessingException {
        var capabilities = ProjectCapabilities.builder()
                .project(Capabilities.builder()
                        .canWrite(true)
                        .canDelete(true)
                        .build())
                .build();

        var json = mapper.writeValueAsString(capabilities);

        assertTrue(json.contains("\"canWrite\":true"));
        assertTrue(json.contains("\"canDelete\":true"));
        assertFalse(json.contains("project"));
        assertFalse(json.contains("file"));
        assertFalse(json.contains("canDeleteProject"));
        assertFalse(json.contains("canDeleteFile"));
    }
}

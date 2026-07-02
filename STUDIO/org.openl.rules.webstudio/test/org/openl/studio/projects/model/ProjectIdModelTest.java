package org.openl.studio.projects.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProjectIdModelTest {

    @Test
    void decodeAcceptsStandardBase64() {
        var encoded = ProjectIdModel.builder().repository("design-flat").projectName("Proj").build().encode();
        var decoded = ProjectIdModel.decode(encoded);

        assertEquals("design-flat", decoded.getRepository());
        assertEquals("Proj", decoded.getProjectName());
    }

    @Test
    void decodeAcceptsUrlSafeBase64() {
        // "r:????" encodes to Base64 containing '/', so this exercises the URL-safe alphabet mapping
        // that callers use to keep the id within a single URL path segment.
        var standard = ProjectIdModel.builder().repository("r").projectName("????").build().encode();
        assertTrue(standard.contains("/") || standard.contains("+"), "expected a Base64 id with '/' or '+'");

        var urlSafe = standard.replace('+', '-').replace('/', '_');
        var decoded = ProjectIdModel.decode(urlSafe);

        assertEquals("r", decoded.getRepository());
        assertEquals("????", decoded.getProjectName());
    }

    @Test
    void encodeUrlSafeStaysWithinOnePathSegment() {
        var id = ProjectIdModel.builder().repository("r").projectName("????").build();
        var urlSafe = id.encodeUrlSafe();

        assertFalse(urlSafe.contains("/"), "URL-safe id must not contain '/'");
        assertFalse(urlSafe.contains("+"), "URL-safe id must not contain '+'");
        assertEquals(id, ProjectIdModel.decode(urlSafe));
    }
}

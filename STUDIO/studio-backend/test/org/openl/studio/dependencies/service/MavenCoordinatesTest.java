package org.openl.studio.dependencies.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A project downloaded from the Maven repository is known in the session by an id made of its coordinates.
 *
 * @author Yury Molchan
 */
class MavenCoordinatesTest {

    private static final MavenCoordinates DOMAIN = new MavenCoordinates("com.example", "domain", "1.0.0");

    @Test
    void readsTheCoordinatesOfAProjectId() {
        assertEquals(DOMAIN, MavenCoordinates.ofProjectId("maven:com.example:domain:1.0.0"));
    }

    @Test
    void theIdOfTheCoordinatesIsReadBackAsThem() {
        assertEquals("maven:com.example:domain:1.0.0", DOMAIN.projectId());
        assertEquals(DOMAIN, MavenCoordinates.ofProjectId(DOMAIN.projectId()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"com.example:domain:1.0.0",
            "ZGVzaWduOkJhbms=",
            "maven:",
            "maven:com.example:domain",
            "maven:com.example:domain:jar:1.0.0",
            "maven::domain:1.0.0",
            "maven:com.example: :1.0.0",
            "maven:com.example:domain:",
            "MAVEN:com.example:domain:1.0.0"})
    void namesNoProjectUnlessThreeCoordinatesFollowThePrefix(String id) {
        assertNull(MavenCoordinates.ofProjectId(id));
    }
}

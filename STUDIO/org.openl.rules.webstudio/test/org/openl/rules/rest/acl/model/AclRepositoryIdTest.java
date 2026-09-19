package org.openl.rules.rest.acl.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import org.openl.security.acl.repository.AclRepositoryType;

class AclRepositoryIdTest {

    private static String encoded(String raw) {
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertRejected(String raw) {
        var value = encoded(raw);
        assertThrows(IllegalArgumentException.class, () -> AclRepositoryId.decode(value));
    }

    @Test
    void decodesTheTypeAloneAndTheTypeWithAnId() {
        var typeOnly = AclRepositoryId.decode(encoded("DESIGN"));
        assertEquals(AclRepositoryType.DESIGN, typeOnly.getType());
        assertNull(typeOnly.getId());

        var withId = AclRepositoryId.decode(encoded("PROD:repo1"));
        assertEquals(AclRepositoryType.PROD, withId.getType());
        assertEquals("repo1", withId.getId());
    }

    @Test
    void rejectsAnIdWithAnEmptyTypeOrRepository() {
        assertRejected(":");
        assertRejected("");
        assertRejected(":repo1");
    }

    @Test
    void rejectsATrailingSeparatorInsteadOfReadingItAsTheTypeAlone() {
        assertRejected("DESIGN:");
    }

    @Test
    void rejectsAnIdCarryingMoreThanTwoParts() {
        assertRejected("DESIGN:repo1:extra");
    }

    @Test
    void encodesBackToTheDecodedValue() {
        assertEquals(encoded("PROD:repo1"), AclRepositoryId.decode(encoded("PROD:repo1")).encode());
        assertEquals(encoded("DESIGN"), AclRepositoryId.decode(encoded("DESIGN")).encode());
    }
}

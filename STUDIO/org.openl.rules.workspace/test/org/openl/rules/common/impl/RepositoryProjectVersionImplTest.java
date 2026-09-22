package org.openl.rules.common.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RepositoryProjectVersionImplTest {

    @Test
    void takesItsNumbersFromTheVersion() {
        var version = new RepositoryProjectVersionImpl(new CommonVersionImpl(1, 2, 3), null);

        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals("3", version.getRevision());
    }

    @Test
    void requiresAVersion() {
        assertThrows(NullPointerException.class, () -> new RepositoryProjectVersionImpl(null, null));
    }
}

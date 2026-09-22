package org.openl.rules.project.abstraction;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.openl.rules.repository.api.FileData;

class AProjectTest {

    @Test
    void requiresTheFileDataItIsReadFrom() {
        assertThrows(NullPointerException.class, () -> new AProject(null, (FileData) null));
    }
}

package org.openl.rules.security.standalone.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class TagTypeTest {

    @Test
    void typesOfTheSameIdAreEqualAndHashAlike() {
        var one = tagType(7L, "Colour");
        var other = tagType(7L, "Size");

        assertEquals(one, other);
        assertEquals(one.hashCode(), other.hashCode());
    }

    @Test
    void typesOfDifferentIdsAreNotEqual() {
        assertNotEquals(tagType(7L, "Colour"), tagType(8L, "Colour"));
    }

    private static TagType tagType(Long id, String name) {
        var tagType = new TagType();
        tagType.setId(id);
        tagType.setName(name);
        return tagType;
    }
}

package org.openl.studio.security.pat.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class PatAuthenticationTokenTest {

    @Test
    void theSameTokenOfTheSameUserIsEqual() {
        var one = new PatAuthenticationToken("admin", null, List.of(), "abcdefghijklmnop");
        var other = new PatAuthenticationToken("admin", null, List.of(), "abcdefghijklmnop");

        assertEquals(one, other);
        assertEquals(one.hashCode(), other.hashCode());
    }

    @Test
    void twoTokensOfOneUserAreTwoAuthentications() {
        var one = new PatAuthenticationToken("admin", null, List.of(), "abcdefghijklmnop");
        var other = new PatAuthenticationToken("admin", null, List.of(), "qrstuvwxyzabcdef");

        assertNotEquals(one, other);
    }
}

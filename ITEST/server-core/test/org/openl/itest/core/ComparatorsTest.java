package org.openl.itest.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ComparatorsTest {

    @Test
    void patternToRegexp() {
        assertEquals("\\$<\\?\\{[#\\d]+\\}\\+\\[[^\uFFFF]*\\]\\.\\{\\\\\\}\\?>\\^", Comparators.patternToRegexp("$<?{###}+[***].{\\}?>^"));
    }
}

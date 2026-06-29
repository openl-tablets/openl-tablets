package org.openl.info;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OpenLVersionTest {

    @Test
    void test() {
        assertEquals(7, OpenLVersion.getBuildInfo().size());
    }

}

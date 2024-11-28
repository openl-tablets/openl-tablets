package org.openl.info;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class OpenLVersionTest {

    @Test
    public void test() {
        assertEquals(7, OpenLVersion.getBuildInfo().size());
    }

}

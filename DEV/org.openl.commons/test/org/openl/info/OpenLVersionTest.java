package org.openl.info;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OpenLVersionTest {

    @Test
    public void test() {
        assertEquals(8, OpenLVersion.getBuildInfo().size());
    }

}

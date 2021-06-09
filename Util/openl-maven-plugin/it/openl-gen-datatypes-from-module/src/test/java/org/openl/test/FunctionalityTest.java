package org.openl.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import com.example.beans.openl.Auto;

public class FunctionalityTest {

    @Test
    public void test() throws Exception {
        assertEquals(Integer.valueOf(150), new Auto().getPower());
    }
}

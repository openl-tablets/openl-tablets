package org.openl.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.beans.openl.Auto;
import org.junit.jupiter.api.Test;

class FunctionalityTest {

    @Test
    void test() throws Exception {
        assertEquals(Integer.valueOf(150), new Auto().getPower());
    }
}

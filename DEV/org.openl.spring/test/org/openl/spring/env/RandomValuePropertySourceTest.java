package org.openl.spring.env;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RandomValuePropertySourceTest {

    @Test
    public void test() {
        var source = new RandomValuePropertySource();
        assertNotNull(source.getProperty("random.uuid"));
        assertNotNull(source.getProperty("random.int"));
        assertNotNull(source.getProperty("random.long"));
        assertNotNull(source.getProperty("random.int(10)"));
        assertNotNull(source.getProperty("random.int(10,20)"));
        assertNotNull(source.getProperty("random.long(10)"));
        assertNotNull(source.getProperty("random.long(10,20)"));
        assertNull(source.getProperty("random.float"));
        assertNull(source.getProperty("random."));
    }

}

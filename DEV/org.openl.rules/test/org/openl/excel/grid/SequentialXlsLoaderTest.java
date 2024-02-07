package org.openl.excel.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class SequentialXlsLoaderTest {
    @Test
    public void getParentAndMergePaths() {
        assertEquals("/f1/f2/m2", SequentialXlsLoader.getParentAndMergePaths("/f1/f2/m1", "m2"));
        assertEquals("/f1/f2/m2", SequentialXlsLoader.getParentAndMergePaths("/f1/f2/", "m2"));
        assertEquals("C:/f1/f2/m2", SequentialXlsLoader.getParentAndMergePaths("C:\\f1\\f2\\", "m2"));
        assertEquals("file://f1/f2/m2", SequentialXlsLoader.getParentAndMergePaths("file://f1/f2/m1", "m2"));
        assertEquals("file://C:/f1/f2/m2",
                SequentialXlsLoader.getParentAndMergePaths("file://C:\\f1\\f2\\", "m2"));

        assertEquals("/f1/f2/m2", SequentialXlsLoader.getParentAndMergePaths("/f1/./././f2/m1", "m2"));
        assertEquals("/f1/f2/m2", SequentialXlsLoader.getParentAndMergePaths("/f1/f2/", "./m2"));
        assertEquals("/m2", SequentialXlsLoader.getParentAndMergePaths("/f1/f2/m1", "../../m2"));
        assertEquals("/f2/m2", SequentialXlsLoader.getParentAndMergePaths("/f1/../f2/", "m2"));
    }
}

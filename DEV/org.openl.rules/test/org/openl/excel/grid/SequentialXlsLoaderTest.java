package org.openl.excel.grid;

import org.junit.Assert;
import org.junit.Test;

public class SequentialXlsLoaderTest {
    @Test
    public void getParentAndMergePaths() {
        Assert.assertEquals("/f1/f2/m2", SequentialXlsLoader.getParentAndMergePaths("/f1/f2/m1", "m2"));
        Assert.assertEquals("/f1/f2/m2", SequentialXlsLoader.getParentAndMergePaths("/f1/f2/", "m2"));
        Assert.assertEquals("C:/f1/f2/m2", SequentialXlsLoader.getParentAndMergePaths("C:\\f1\\f2\\", "m2"));
        Assert.assertEquals("file://f1/f2/m2", SequentialXlsLoader.getParentAndMergePaths("file://f1/f2/m1", "m2"));
        Assert.assertEquals("file://C:/f1/f2/m2",
            SequentialXlsLoader.getParentAndMergePaths("file://C:\\f1\\f2\\", "m2"));

        Assert.assertEquals("/f1/f2/m2", SequentialXlsLoader.getParentAndMergePaths("/f1/./././f2/m1", "m2"));
        Assert.assertEquals("/f1/f2/m2", SequentialXlsLoader.getParentAndMergePaths("/f1/f2/", "./m2"));
        Assert.assertEquals("/m2", SequentialXlsLoader.getParentAndMergePaths("/f1/f2/m1", "../../m2"));
        Assert.assertEquals("/f2/m2", SequentialXlsLoader.getParentAndMergePaths("/f1/../f2/", "m2"));
    }
}

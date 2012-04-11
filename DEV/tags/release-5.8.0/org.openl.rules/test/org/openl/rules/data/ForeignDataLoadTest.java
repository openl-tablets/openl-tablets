package org.openl.rules.data;

import static junit.framework.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestHelper;

public class ForeignDataLoadTest {

    public interface ITestI {
        Type2[] getData2();
    }

    @Test
    public void testForeignDataLoad() {
        File xlsFile = new File("test/rules/data/ForeignDataLoadTest.xls");
        TestHelper<ITestI> testHelper;
        testHelper = new TestHelper<ITestI>(xlsFile, ITestI.class);

        ITestI instance = testHelper.getInstance();

        Type2[] data = instance.getData2();
        assertEquals(4, data.length);

        assertEquals(1, data[0].getTypes().length);
        assertEquals(0, data[1].getTypes().length);
        assertEquals(2, data[2].getTypes().length);
        assertEquals(1, data[3].getTypes().length);
    }
}

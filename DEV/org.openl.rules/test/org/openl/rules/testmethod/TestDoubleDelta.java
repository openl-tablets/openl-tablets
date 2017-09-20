package org.openl.rules.testmethod;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.TestHelper;

/*
 * @author PTarasevich
 */

public class TestDoubleDelta {
    private static final String FILE_NAME = "test/rules/testmethod/DoubleDeltaTest.xlsx";

    public interface ITestDouble {
        TestUnitsResults testSSTest();
        TestUnitsResults geTestDoubleTest();
        TestUnitsResults geTestDoubleTest2();
    }

    @Before
    public void before() {
        System.setProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, "true");
    }

    @Test
    public void testSSTest() {
        File xlsFile = new File(FILE_NAME);
        TestHelper<ITestDouble> testHelper = new TestHelper<ITestDouble>(xlsFile, ITestDouble.class);

        ITestDouble instance = testHelper.getInstance();
        TestUnitsResults result = instance.testSSTest();

        assertEquals(0, result.getNumberOfFailures());
    }

    @Test
    public void testDoubleTest() {
        File xlsFile = new File(FILE_NAME);
        TestHelper<ITestDouble> testHelper = new TestHelper<ITestDouble>(xlsFile, ITestDouble.class);

        ITestDouble instance = testHelper.getInstance();
        TestUnitsResults result = instance.geTestDoubleTest();
        assertEquals(2, result.getNumberOfFailures());
    }

    @Test
    public void testDoubleTest2() {
        File xlsFile = new File(FILE_NAME);
        TestHelper<ITestDouble> testHelper = new TestHelper<ITestDouble>(xlsFile, ITestDouble.class);

        ITestDouble instance = testHelper.getInstance();
        TestUnitsResults result = instance.geTestDoubleTest2();
        assertEquals(1, result.getNumberOfFailures());
    }
}

package org.openl.rules.cmatch;

import static junit.framework.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestHelper;
import org.openl.rules.TestUtils;
import org.openl.rules.cmatch.algorithm.IMatchAlgorithmCompilerBuilder;
import org.openl.rules.cmatch.algorithm.MatchAlgorithmCompilerBuilder;
import org.openl.rules.cmatch.algorithm.MatchAlgorithmFactory;

public class Test0 {
    public interface ITestI {
        int runColumnMatch(int i);
    }

    @Test
    public void test1() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/cmatch0/match0-1.xls");                
                new TestHelper<ITestI>(xlsFile, ITestI.class);
            }
        }, "Unsufficient rows. At least 4 are expected!");
    }

    @Test
    public void test2() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/cmatch0/match0-2.xls");             
                new TestHelper<ITestI>(xlsFile, ITestI.class);
            }
        }, "Name cannot be empty!", "cell=B7");
    }

    @Test
    public void test3() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/cmatch0/match0-3.xls");                
                new TestHelper<ITestI>(xlsFile, ITestI.class);
            }
        }, "Name cannot be empty!", "cell=B8");
    }
    
    @Test
    public void testDublicateColumn() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/cmatch0/Test_Dublicate_Column.xls");                
                new TestHelper<ITestI>(xlsFile, ITestI.class);
            }
        }, "Duplicate column 'values'!");
    }

    @Test
    public void test4() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/cmatch0/match0-4.xls");                
                new TestHelper<ITestI>(xlsFile, ITestI.class);
            }
        }, "org.openl.exception.OpenLRuntimeException", "cell=E6");
    }

    @Test
    public void test5() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/cmatch0/match0-5.xls");                
                new TestHelper<ITestI>(xlsFile, ITestI.class);
            }
        }, "Cannot find algorithm for name 'ERROR'!", "range=B3:L7");
    }

    @Test
    public void test6() {
        IMatchAlgorithmCompilerBuilder old = MatchAlgorithmFactory.getDefaultBuilder();
        MatchAlgorithmFactory.setDefaultBuilder(null);

        Exception ex = null;
        try {
            File xlsFile = new File("test/rules/cmatch0/match0-6.xls");            
            new TestHelper<ITestI>(xlsFile, ITestI.class);
        } catch (Exception e) {
            ex = e;
        } finally {
            MatchAlgorithmFactory.setDefaultBuilder(old);
        }

        TestUtils.assertEx(ex, "Default algorithm builder was not defined!", "range=B3:L7");
    }

    @Test
    public void test7() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/cmatch0/match0-7.xls");                
                new TestHelper<ITestI>(xlsFile, ITestI.class);
            }
        }, "Illegal header format!", "range=B3:L7");
    }

    @Test
    public void test8() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/cmatch0/match0-8.xls");                
                new TestHelper<ITestI>(xlsFile, ITestI.class);
            }
        }, "Unsufficient rows.", "range=B3:L4");
    }

    @Test
    public void testCustom() {
        MatchAlgorithmFactory.registerBuilder("ALGORITHM", new MatchAlgorithmCompilerBuilder());

        File xlsFile = new File("test/rules/ColumnMatch.xls");
        TestHelper<ITestColumnMatch> testHelper;
        testHelper = new TestHelper<ITestColumnMatch>(xlsFile, ITestColumnMatch.class);

        ITestColumnMatch test = testHelper.getInstance();
        int real = test.runColumnMatch("OO", "Y", 100);
        assertEquals(2, real);
    }

}

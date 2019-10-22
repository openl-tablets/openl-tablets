package org.openl.rules.cmatch;

import static org.junit.Assert.assertEquals;
import static org.openl.rules.TestUtils.assertEx;

import org.junit.After;
import org.junit.Test;
import org.openl.rules.TestUtils;
import org.openl.rules.cmatch.algorithm.MatchAlgorithmCompilerBuilder;
import org.openl.rules.cmatch.algorithm.MatchAlgorithmFactory;

public class Test0 {

    @After
    public void restore() {
        MatchAlgorithmFactory.setDefaultBuilder(new MatchAlgorithmCompilerBuilder());
    }

    @Test
    public void test1() {
        assertEx("test/rules/cmatch0/match0-1.xls", "Unsufficient rows. At least 4 are expected.");
    }

    @Test
    public void test2() {
        assertEx("test/rules/cmatch0/match0-2.xls", "Name cannot be empty.", "cell=B7");
    }

    @Test
    public void test3() {
        assertEx("test/rules/cmatch0/match0-3.xls", "Name cannot be empty.", "cell=B8");
    }

    @Test
    public void testDublicateColumn() {
        assertEx("test/rules/cmatch0/Test_Dublicate_Column.xls", "Duplicate column 'values'.");
    }

    @Test
    public void test4() {
        assertEx("test/rules/cmatch0/match0-4.xls", "Cannot convert an empty String to numeric type", "cell=E6");
    }

    @Test
    public void test5() {
        assertEx("test/rules/cmatch0/match0-5.xls", "Cannot find algorithm for name 'ERROR'.", "range=B3:L7");
    }

    @Test
    public void test6() {
        MatchAlgorithmFactory.setDefaultBuilder(null);
        assertEx("test/rules/cmatch0/match0-6.xls", "Default algorithm builder is not defined.", "range=B3:L7");
    }

    @Test
    public void test7() {
        assertEx("test/rules/cmatch0/match0-7.xls", "Illegal header format.", "range=B3:L7");
    }

    @Test
    public void test8() {
        assertEx("test/rules/cmatch0/match0-8.xls", "Unsufficient rows.", "range=B3:L4");
    }

    @Test
    public void testCustom() {
        MatchAlgorithmFactory.registerBuilder("ALGORITHM", new MatchAlgorithmCompilerBuilder());

        ITestColumnMatch test = TestUtils.create("test/rules/ColumnMatch.xls", ITestColumnMatch.class);
        int real = test.runColumnMatch("OO", "Y", 100);
        assertEquals(2, real);
    }

    public interface ITestI {
        int runColumnMatch(int i);
    }

}

package org.openl.rules.datatype;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestHelper;
import org.openl.rules.helpers.IntRange;

public class AliasDatatypeTest {

	private static String src = "test/rules/datatype/AliasDatatypeTest.xlsx";

	public interface ITest {
		int test1(String state);
		int test2(int age);
		int test3(IntRange age);
		int method1();
		int method2();
		boolean method3(int z);
		
		int testStringAliasType(String state);
        int testAliasTypeAsArrays(String state);
		int testIntAliasType(int i);
		int testIntRangeAliasType2(int i);
	}

	@Test
	public void test1() {
		File xlsFile = new File(src);
		TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile,
				ITest.class);

		ITest instance = testHelper.getInstance();
		int res = instance.test1("CA");
		assertEquals(1, res);
	}
	
	@Test
	public void test11() {
		File xlsFile = new File(src);
		TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile,
				ITest.class);

		ITest instance = testHelper.getInstance();
		int res = instance.testStringAliasType("CA");
		assertEquals(1, res);
	}

	@Test(expected = RuntimeException.class)
	public void test2() {
		File xlsFile = new File(src);
		TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile,
				ITest.class);

		ITest instance = testHelper.getInstance();
		instance.test1("Something that doesn't belong to domain");
	}

	@Test
	public void test3() {
		File xlsFile = new File(src);
		TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile,
				ITest.class);

		ITest instance = testHelper.getInstance();
		int res = instance.test2(1);
		assertEquals(3, res);
	}
	
	
	@Test
	public void test31() {
		File xlsFile = new File(src);
		TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile,
				ITest.class);

		ITest instance = testHelper.getInstance();
		int res = instance.testIntAliasType(1);
		assertEquals(3, res);
	}
	

	@Test(expected = Exception.class)
	public void test4() {
		File xlsFile = new File(src);
		TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile,
				ITest.class);

		ITest instance = testHelper.getInstance();
		instance.test3(new IntRange(1, 3));
	}
	
	@Test
	public void test41() {
		File xlsFile = new File(src);
		TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile,
				ITest.class);

		ITest instance = testHelper.getInstance();
		int res = instance.testIntRangeAliasType2(15);
		assertEquals(1, res);

		res = instance.testIntRangeAliasType2(1000);
		assertEquals(0, res);
	}

	@Test(expected = RuntimeException.class)
	public void test5() {
		File xlsFile = new File(src);
		TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile,
				ITest.class);

		ITest instance = testHelper.getInstance();
		instance.method1();
	}

	@Test
	public void test6() {
		File xlsFile = new File(src);
		TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile,
				ITest.class);

		ITest instance = testHelper.getInstance();
		int res = instance.method2();
		assertEquals(1, res);
	}

    @Test
    public void test7() {
        File xlsFile = new File(src);
        TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile,
                ITest.class);

        ITest instance = testHelper.getInstance();
        boolean res = instance.method3(5);
        assertEquals(true, res);

        res = instance.method3(-1);
        assertEquals(false, res);
    }

    @Test
    public void testArrays() {
        File xlsFile = new File(src);
        TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile,
                ITest.class);

        ITest instance = testHelper.getInstance();
        int res = instance.testAliasTypeAsArrays("AR");
        assertEquals(1, res);

        res = instance.testAliasTypeAsArrays("NY");
        assertEquals(2, res);
    }
}

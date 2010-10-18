package org.openl.rules.dt;

import junit.framework.TestCase;

import org.openl.meta.DoubleValue;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.runtime.RuleEngineFactory;
import org.openl.runtime.EngineFactory;

public class LocalParametersDeclarationTest extends TestCase {

    public interface ILocalParametersDeclarationTest {

        String test1(int age);

        String test2(String ageType);

        String test3(String ageType);

        String test4(boolean hadTraining);

        String test5(int age);

        String test6(int age);

        void test7(int age);

        void test8(int age);
        
        String test9(int age);
        
        int test10(int age);
        
        DoubleValue test11(int age);
        
        void test12(int age);
    
        String test13(int age);
        
        int test14(int age);
        
        IntRange test15(int age);

        String test16(double age);
        
        double test17(double age);
        
        DoubleRange test18(double age);

    }

    private static String src = "test/rules/dt/LocalParametersDeclarationTest.xls";

    private ILocalParametersDeclarationTest test;

    @Override
    protected void setUp() throws Exception {
        EngineFactory<ILocalParametersDeclarationTest> engineFactory = new EngineFactory<ILocalParametersDeclarationTest>(RuleEngineFactory.RULE_OPENL_NAME,
            src,
            ILocalParametersDeclarationTest.class);

        test = engineFactory.makeInstance();
    }

    public void testParamsDeclarationWithDifferentTypes() {
        String result = test.test1(10);
        assertEquals("Not Eligible", result);

        result = test.test1(40);
        assertEquals("Eligible", result);
    }

    public void testAnonymousParamUsing() {

        String result = test.test2("Young Driver");
        assertEquals("Not Eligible", result);

        result = test.test2("Senior Driver");
        assertEquals("Not Eligible", result);

        result = test.test2("Just Driver");
        assertEquals("Eligible", result);
    }

    public void testWithoutParamNameDeclaration() {

        String result = test.test3("Young Driver");
        assertEquals("Not Eligible", result);

        result = test.test3("Senior Driver");
        assertEquals("Not Eligible", result);

        result = test.test3("Just Driver");
        assertEquals("Eligible", result);
    }

    public void testWithoutParamDeclaration1() {

        String result = test.test4(false);
        assertEquals("Not Eligible", result);

        result = test.test4(true);
        assertEquals("Eligible", result);
    }
    
    public void testWithoutParamDeclaration2() {

        String result = test.test13(10);
        assertEquals("Not Eligible", result);

        result = test.test13(40);
        assertEquals("Eligible", result);
    }
    
    public void testWithoutParamDeclaration3() {

        String result = test.test16(10);
        assertEquals("Not Eligible", result);

        result = test.test16(40);
        assertEquals("Eligible", result);
    }

    public void testFullParamsDeclaration() {
        String result = test.test5(10);
        assertEquals("Not Eligible", result);

        result = test.test5(40);
        assertEquals("Eligible", result);
    }

    public void testReturnParamsDeclaration() {
        String result = test.test6(10);
        assertEquals("Not Eligible", result);

        result = test.test6(40);
        assertEquals("Eligible", result);
    }

    public void testActionParamsDeclaration() {
        test.test7(10);
        test.test7(40);
    }

    public void testEmptyActionParamsDeclaration() {
        test.test8(10);
        test.test8(40);
    }

    public void testSimplifiedReturnParamsDeclaration1() {

        String result = test.test9(10);
        assertEquals("1", result);

        result = test.test9(40);
        assertEquals("2", result);
    }
    
    public void testSimplifiedReturnParamsDeclaration2() {

        int result = test.test10(10);
        assertEquals(1, result);

        result = test.test10(40);
        assertEquals(2, result);
    }
    
    public void testSimplifiedReturnParamsDeclaration3() {

        DoubleValue result = test.test11(10);
        assertEquals(1.0, result.doubleValue());

        result = test.test11(40);
        assertEquals(2.0, result.doubleValue());
    }

    public void testSimplifiedReturnParamsDeclaration4() {

        test.test12(10);
        test.test12(40);
    }

    public void testSimplifiedReturnParamsDeclaration5() {
        int returnValue = test.test14(10);
        assertEquals(1, returnValue);
        returnValue = test.test14(40);
        assertEquals(2, returnValue);
    }

    public void testSimplifiedReturnParamsDeclaration6() {
        IntRange returnValue = test.test15(10);
        assertEquals(new IntRange(1), returnValue);
        returnValue = test.test15(40);
        assertEquals(new IntRange(2), returnValue);
    }

    public void testSimplifiedReturnParamsDeclaration7() {
        double returnValue = test.test17(10);
        assertEquals(1.0, returnValue);
        returnValue = test.test17(40);
        assertEquals(2.0, returnValue);
    }

    public void testSimplifiedReturnParamsDeclaration8() {
        DoubleRange returnValue = test.test18(10);
        assertEquals(new DoubleRange(1.0, 1.0), returnValue);
        returnValue = test.test18(40);
        assertEquals(new DoubleRange(2.0, 2.0), returnValue);
    }

}

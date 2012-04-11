package org.openl.binding;

import java.net.URL;

import junit.framework.TestCase;

import org.openl.meta.DoubleValue;
import org.openl.rules.runtime.RuleEngineFactory;

public class TypesCastTest extends TestCase {

    private static final String FILE_NAME = "org/openl/binding/CastTest.xls";

    public interface ITest {
        float[] testExplicitCastsForLiterals();
        DoubleValue testAutoCast(double value);
        boolean operatorTest1();
        boolean operatorTest2();
        boolean operatorTest3();
        boolean operatorTest4();
    }
    
    public void testExplicitCastsForLiterals(){
        URL url = this.getClass().getClassLoader().getResource(FILE_NAME);
        RuleEngineFactory<ITest> engineFactory = new RuleEngineFactory<ITest>(url.getPath(), ITest.class);

        ITest instance = (ITest) engineFactory.makeInstance();
        float[] result = instance.testExplicitCastsForLiterals();

        assertEquals(4, result.length);
    }

    public void testAutoCast() {

        URL url = this.getClass().getClassLoader().getResource(FILE_NAME);
        RuleEngineFactory<ITest> engineFactory = new RuleEngineFactory<ITest>(url.getPath(), ITest.class);

        ITest instance = (ITest) engineFactory.makeInstance();
        DoubleValue result = instance.testAutoCast(10.1);

        assertEquals(10.1, result.getValue());
    }
    
    public void testOperator1() {

        URL url = this.getClass().getClassLoader().getResource(FILE_NAME);
        RuleEngineFactory<ITest> engineFactory = new RuleEngineFactory<ITest>(url.getPath(), ITest.class);

        ITest instance = (ITest) engineFactory.makeInstance();
        boolean result = instance.operatorTest1();

        assertEquals(true, result);
    }

    public void testOperator2() {

        URL url = this.getClass().getClassLoader().getResource(FILE_NAME);
        RuleEngineFactory<ITest> engineFactory = new RuleEngineFactory<ITest>(url.getPath(), ITest.class);

        ITest instance = (ITest) engineFactory.makeInstance();
        boolean result = instance.operatorTest2();

        assertEquals(true, result);
    }

    public void testOperator3() {

        URL url = this.getClass().getClassLoader().getResource(FILE_NAME);
        RuleEngineFactory<ITest> engineFactory = new RuleEngineFactory<ITest>(url.getPath(), ITest.class);

        ITest instance = (ITest) engineFactory.makeInstance();
        boolean result = instance.operatorTest3();

        assertEquals(true, result);
    }
    
    public void testOperator4() {

        URL url = this.getClass().getClassLoader().getResource(FILE_NAME);
        RuleEngineFactory<ITest> engineFactory = new RuleEngineFactory<ITest>(url.getPath(), ITest.class);

        ITest instance = (ITest) engineFactory.makeInstance();
        boolean result = instance.operatorTest4();

        assertEquals(true, result);
    }
    
}

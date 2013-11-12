package org.openl.binding;

import java.net.URISyntaxException;
import java.net.URL;

import junit.framework.TestCase;

import org.openl.meta.DoubleValue;
import org.openl.rules.runtime.RulesEngineFactory;

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
    
    public void testExplicitCastsForLiterals() throws URISyntaxException{
        URL url = this.getClass().getClassLoader().getResource(FILE_NAME);
        RulesEngineFactory<ITest> engineFactory = new RulesEngineFactory<ITest>(url.toURI().getPath(), ITest.class);

        ITest instance = engineFactory.newEngineInstance();
        float[] result = instance.testExplicitCastsForLiterals();

        assertEquals(4, result.length);
    }

    public void testAutoCast() throws URISyntaxException {

        URL url = this.getClass().getClassLoader().getResource(FILE_NAME);
        RulesEngineFactory<ITest> engineFactory = new RulesEngineFactory<ITest>(url.toURI().getPath(), ITest.class);

        ITest instance = engineFactory.newEngineInstance();
        DoubleValue result = instance.testAutoCast(10.1);

        assertEquals(10.1, result.getValue());
    }
    
    public void testOperator1() throws URISyntaxException {

        URL url = this.getClass().getClassLoader().getResource(FILE_NAME);
        RulesEngineFactory<ITest> engineFactory = new RulesEngineFactory<ITest>(url.toURI().getPath(), ITest.class);

        ITest instance = (ITest) engineFactory.newEngineInstance();
        boolean result = instance.operatorTest1();

        assertEquals(true, result);
    }

    public void testOperator2() throws URISyntaxException {

        URL url = this.getClass().getClassLoader().getResource(FILE_NAME);
        RulesEngineFactory<ITest> engineFactory = new RulesEngineFactory<ITest>(url.toURI().getPath(), ITest.class);

        ITest instance = engineFactory.newEngineInstance();
        boolean result = instance.operatorTest2();

        assertEquals(true, result);
    }

    public void testOperator3() throws URISyntaxException {

        URL url = this.getClass().getClassLoader().getResource(FILE_NAME);
        RulesEngineFactory<ITest> engineFactory = new RulesEngineFactory<ITest>(url.toURI().getPath(), ITest.class);

        ITest instance = engineFactory.newEngineInstance();
        boolean result = instance.operatorTest3();

        assertEquals(true, result);
    }
    
    public void testOperator4() throws URISyntaxException {

        URL url = this.getClass().getClassLoader().getResource(FILE_NAME);
        RulesEngineFactory<ITest> engineFactory = new RulesEngineFactory<ITest>(url.toURI().getPath(), ITest.class);

        ITest instance = engineFactory.newEngineInstance();
        boolean result = instance.operatorTest4();

        assertEquals(true, result);
    }
    
}

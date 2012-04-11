package org.openl.binding;

import java.net.URL;

import junit.framework.TestCase;

import org.openl.meta.DoubleValue;
import org.openl.rules.runtime.RuleEngineFactory;

public class TypesCastTest extends TestCase {

    private static final String FILE_NAME = "org/openl/binding/AutoCastTest.xls";

    public interface ITest {
        DoubleValue testRule(double value);
    }

    public void testAutoCast() {

        URL url = this.getClass().getClassLoader().getResource(FILE_NAME);
        RuleEngineFactory<ITest> engineFactory = new RuleEngineFactory<ITest>(url.getPath(), ITest.class);

        ITest instance = (ITest) engineFactory.makeInstance();
        DoubleValue result = instance.testRule(10.1);

        assertEquals(10.1, result.getValue());
    }
}

package org.openl.rules.method;

import java.io.IOException;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.types.IOpenMethod;

public class ConstructorMethodsTest {

    private static final String SRC = "test/rules/Main.xls";

    private RulesEngineFactory<Object> engineFactory;

    @Before
    public void init() throws IOException {
        engineFactory = new RulesEngineFactory<Object>(SRC);
    }

    @Test
    public void test() {
        Iterable<IOpenMethod> constructors = engineFactory.getCompiledOpenClass().getOpenClass().constructors();
        Iterator<IOpenMethod> itr = constructors.iterator();
        Assert.assertTrue(itr.hasNext());
        Assert.assertTrue(itr.next().isConstructor());

        Iterable<IOpenMethod> methods = engineFactory.getCompiledOpenClass().getOpenClass().methods("Main");
        Iterator<IOpenMethod> itr2 = methods.iterator();
        Assert.assertTrue(itr2.hasNext());
        Assert.assertFalse(itr2.next().isConstructor());

    }
}

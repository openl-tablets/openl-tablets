package org.openl.rules.method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.types.IOpenMethod;

public class ConstructorMethodsTest {

    private static final String SRC = "test/rules/Main.xls";

    private RulesEngineFactory<Object> engineFactory;

    @BeforeEach
    public void init() throws IOException {
        engineFactory = new RulesEngineFactory<>(SRC);
    }

    @Test
    public void test() {
        Iterable<IOpenMethod> constructors = engineFactory.getCompiledOpenClass().getOpenClass().constructors();
        Iterator<IOpenMethod> itr = constructors.iterator();
        assertTrue(itr.hasNext());
        assertTrue(itr.next().isConstructor());

        Iterable<IOpenMethod> methods = engineFactory.getCompiledOpenClass().getOpenClass().methods("Main");
        Iterator<IOpenMethod> itr2 = methods.iterator();
        assertTrue(itr2.hasNext());
        assertFalse(itr2.next().isConstructor());

    }
}

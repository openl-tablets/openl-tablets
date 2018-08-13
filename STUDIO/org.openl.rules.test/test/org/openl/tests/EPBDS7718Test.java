package org.openl.tests;

import java.io.File;

import org.junit.Test;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.syntax.exception.CompositeOpenlException;

public class EPBDS7718Test {
    private static final String SRC = "test-resources/tests/EPBDS-7718_NPE.xlsx";

    @Test(expected = CompositeOpenlException.class)
    public void test() {
            RulesEngineFactory engineFactory = new RulesEngineFactory(URLSourceCodeModule.toUrl(new File(SRC)));
            Object instance = engineFactory.newEngineInstance();
    }

}

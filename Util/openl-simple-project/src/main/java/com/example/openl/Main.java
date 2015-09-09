package com.example.openl;

import org.openl.rules.runtime.RulesEngineFactory;

import java.io.File;

/**
 * This class shows how to execute OpenL Tablets methods using Java wrapper.
 * Looks really simple...
 */
public class Main {

    public static void main(String[] args) {
        File xlsFile = new File("rules/TemplateRules.xls");

        // Wrapper - is generated interface from TemplateRules.xls using maven openl:generate goal.
        RulesEngineFactory<Wrapper> engineFactory = new RulesEngineFactory<Wrapper>(xlsFile, Wrapper.class);

        Wrapper instance = engineFactory.newEngineInstance();
        String result = instance.hello(10);
        System.out.println(result);
    }
}

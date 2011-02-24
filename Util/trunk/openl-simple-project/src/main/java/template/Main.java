package template;

import org.openl.rules.runtime.RuleEngineFactory;

import java.io.File;

/**
 * This class shows how to execute OpenL Tablets methods using Java wrapper.
 * Looks really simple...
 */
public class Main {

    public static void main(String[] args) {
        File xlsFile = new File("src/main/resources/rules/TemplateRules.xls");

        RuleEngineFactory<Wrapper> engineFactory = new RuleEngineFactory<Wrapper>(xlsFile, Wrapper.class);

        Wrapper instance = engineFactory.makeInstance();
        instance.hello1(10);
    }
}

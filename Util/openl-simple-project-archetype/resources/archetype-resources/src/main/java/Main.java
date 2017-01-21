#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import org.openl.rules.runtime.RulesEngineFactory;

import java.io.File;

/**
 * This class shows how to execute OpenL Tablets methods using Java wrapper.
 * Looks really simple...
 */
public class Main {

    public static void main(String[] args) {
        // Wrapper - is generated interface from TemplateRules.xls using maven openl:generate goal.
        RulesEngineFactory<Service> engineFactory = new RulesEngineFactory<Service>("rules/TemplateRules.xls", Service.class);

        Service instance = engineFactory.newEngineInstance();
        String result = instance.hello(10);
        System.out.println(result);
    }
}

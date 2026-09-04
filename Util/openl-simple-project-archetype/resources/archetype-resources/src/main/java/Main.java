#set($symbol_pound='#')
        #set($symbol_dollar='$')
        #set($symbol_escape='\' )
        package ${package};

import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;

/**
 * This class shows how to execute OpenL Tablets methods using Java interface.
 * Looks really simple...
 */
public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: Main <hour>");
            System.exit(1);
        }
        var hour = Integer.parseInt(args[0]);
        var result = run(hour);
        System.out.println(result);
    }

    public static String run(int hour) throws Exception {
        // Service - is generated interface from TemplateRules.xls using maven openl:generate goal.
        var factoryBuilder = new SimpleProjectEngineFactoryBuilder<Service>();
        var factory = factoryBuilder.setProject("openl")
                .setInterfaceClass(Service.class)
                .build();

        var instance = factory.newInstance();
        return instance.hello(hour);

    }
}

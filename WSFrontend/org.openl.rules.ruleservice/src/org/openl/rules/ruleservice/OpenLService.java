package org.openl.rules.ruleservice;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.openl.runtime.ASMProxyFactory;
import org.openl.spring.env.PropertySourcesLoader;

/**
 * A utility invoker of the OpenL rules. It is easy to use with AWS lambdas or within Apache Spark jobs.
 * OpenL RuleService is lazily initialized by the first call of the {@linkplain #call(String, String, Object...)}
 * and it can be reset for reinitialization by {@linkplain  #reset()} method.
 */
public class OpenLService {

    static RulesFrontend rulesFrontend; // non-private opened for testing purposes
    private static ClassPathXmlApplicationContext context;

    /**
     * Invokes an OpenL method of a deployed service with specified parameters.
     *
     * @param serviceName Name of deployed service.
     * @param ruleName    Method name to execute, a-ka rule name.
     * @param params      Parameters for method execution.
     * @return Result of execution
     */
    public static Object call(String serviceName, String ruleName, Object... params) throws Exception {
        return getRulesFrontend().execute(serviceName, ruleName, params);
    }

    /**
     * Returns the object instance of the OpenL rules.
     *
     * @param serviceName Name of deployed service.
     * @return the OpenL rules object instance
     */
    public static <T> T get(String serviceName) throws Exception {
        var service = getRulesFrontend().findServiceByName(serviceName);
        return service != null ? (T) service.getServiceBean() : null;
    }

    /**
     * Returns the proxied instance of the OpenL rules is decorated by interface.
     *
     * @param serviceName Name of deployed service.
     * @return the OpenL rules proxy instance
     */
    public static <T> T proxy(String serviceName, Class<T> proxyInterface) throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return ASMProxyFactory.newProxyInstance(cl, (method, args) -> {
            return getRulesFrontend().execute(serviceName, method.getName(), method.getParameterTypes(), args);
        }, proxyInterface);
    }

    /**
     * Reset the previous initialized Rules Frontend instance.
     */
    public static void reset() {
        if (rulesFrontend != null) {
            synchronized (OpenLService.class) {
                if (rulesFrontend != null) {
                    context.close();
                    context = null;
                    rulesFrontend = null;
                }
            }
        }
    }

    /**
     * Lazy initialization of the Rules Frontend instance.
     */
    private static RulesFrontend getRulesFrontend() {
        if (rulesFrontend == null) {
            synchronized (OpenLService.class) {
                if (rulesFrontend == null) {
                    var springContext = new ClassPathXmlApplicationContext();
                    springContext.setConfigLocations("classpath:openl-ruleservice-beans.xml");
                    new PropertySourcesLoader().initialize(springContext);
                    springContext.refresh();
                    context = springContext;
                    rulesFrontend = springContext.getBean(RulesFrontend.class);
                }
            }
        }
        return rulesFrontend;
    }
}

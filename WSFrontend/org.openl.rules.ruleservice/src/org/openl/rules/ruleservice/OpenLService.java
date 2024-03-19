package org.openl.rules.ruleservice;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.openl.rules.serialization.JsonUtils;
import org.openl.runtime.ASMProxyFactory;
import org.openl.spring.env.PropertySourcesLoader;

/**
 * A utility invoker of the OpenL rules. It is easy to use with AWS lambdas or within Apache Spark jobs.
 * OpenL RuleService is lazily initialized by the first call of the {@linkplain #call(String, String, Object...)}
 * and it can be reset for reinitialization by {@linkplain  #reset()} method.
 */
public class OpenLService {

    private static final Class<?>[] EMPTY_CLASSES = new Class[0];
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

    public static String callJSON(String serviceName, String ruleName, String json) throws Exception {
        var instance = get(serviceName);
        if (instance == null) {
            throw new IllegalArgumentException(String.format("Service '%s' is not found.", serviceName));
        }
        ArrayList<Method> methods = new ArrayList<>(2);
        for (Method method : instance.getClass().getMethods()) {
            if (method.getName().equals(ruleName)) {
                methods.add(method);
            }
        }
        if (methods.isEmpty()) {
            throw new IllegalArgumentException(String.format("Method '%s' is not found in service '%s'.", ruleName, serviceName));
        }

        if (methods.size() > 1) {
            throw new IllegalArgumentException(String.format("Non-unique '%s' method name in service '%s'. There are %d methods with the same name.", ruleName, serviceName, methods.size()));
        }

        var caller = methods.get(0);

        var args = new Object[caller.getParameterCount()];

        var mapper = JsonUtils.getCachedObjectMapper(caller, EMPTY_CLASSES);
        if (json != null) {
            if (caller.getParameterCount() == 1) {
                args[0] = mapper.readValue(json, caller.getParameterTypes()[0]);
            } else {
                var tree = mapper.readTree(json);
                for (int i = 0; i < caller.getParameterCount(); i++) {
                    var parameter = caller.getParameters()[i];
                    var name = parameter.getName();
                    var type = parameter.getType();
                    var node = tree.get(name);
                    args[i] = mapper.treeToValue(node, type);
                }
            }
        }
        var result = caller.invoke(instance, args);
        return result == null || result instanceof String ? (String) result : mapper.writeValueAsString(result);

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
        return ASMProxyFactory.newProxyInstance(cl,
                (method, args) -> getRulesFrontend().execute(serviceName, method.getName(), method.getParameterTypes(), args),
                proxyInterface
        );
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

package org.openl.rules.ruleservice;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.openl.binding.MethodUtil;
import org.openl.rules.ruleservice.core.ExceptionType;
import org.openl.rules.ruleservice.core.RuleServiceWrapperException;
import org.openl.rules.ruleservice.core.ServiceInvocationAdvice;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.openl.runtime.ASMProxyFactory;
import org.openl.spring.env.PropertySourcesLoader;

/**
 * A utility invoker of the OpenL rules. It is easy to use with AWS lambdas or within Apache Spark jobs.
 * OpenL RuleService is lazily initialized by the first call of the {@linkplain #call(String, String, Object...)}
 * and it can be reset for reinitialization by {@linkplain  #reset()} method.
 */
public class OpenLService {

    static volatile RulesFrontend rulesFrontend; // non-private opened for testing purposes
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
        if (params == null) {
            params = new Object[0];
        }
        var types = Arrays.stream(params).map(x -> x != null ? x.getClass() : null).toArray(Class[]::new);
        return execute(serviceName, ruleName, types, params);
    }

    /**
     * Invokes an OpenL method of a deployed service with parameters are presented as a JSON string. If the method has
     * only one argument, then the JSON will be parsed as is into this argument.
     *
     * @param serviceName Name of deployed service.
     * @param ruleName    Method name to execute, a-ka rule name.
     * @param json        Parameters for method execution in a JSON object, where first level keys are argument names.
     * @return Result of execution as a JSON string
     * @throws Exception any exception
     * @see #tryJSON(String, String, String)
     */
    public static String callJSON(String serviceName, String ruleName, String json) throws Exception {
        Invoker invoker = getInvoker(serviceName, ruleName, json);
        var result = invoker.invoke();
        return result == null || result instanceof String ? (String) result : invoker.mapper.writeValueAsString(result);

    }

    /**
     * Invokes an OpenL method of a deployed service with parameters are presented as a JSON string. If the method has
     * only one argument, then the JSON will be parsed as is into this argument.
     *
     * @param serviceName Name of deployed service.
     * @param ruleName    Method name to execute, a-ka rule name.
     * @param json        Parameters for method execution in a JSON object, where first level keys are argument names.
     * @return Result of execution or an exception as a JSON string.
     * @see #callJSON(String, String, String)
     */
    public static String tryJSON(String serviceName, String ruleName, String json) {
        try {
            Invoker invoker = getInvoker(serviceName, ruleName, json);
            try {
                var result = invoker.invoke();
                var x = invoker.mapper.createObjectNode();
                x.putPOJO("result", result);
                x.putNull("error");
                return invoker.mapper.writeValueAsString(x);
            } catch (Exception ex) {
                return errorJSON(invoker.mapper, ex);
            }
        } catch (Exception ex) {
            return errorJSON(null, ex);
        }
    }

    private static Invoker getInvoker(String serviceName, String ruleName, String json) throws Exception {
        var service = getService(serviceName);
        if (service == null) {
            throw new IllegalArgumentException(String.format("Service '%s' is not found.", serviceName));
        }
        var instance = service.getServiceBean();

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

        var mapper = service.getServiceContext().getBean(ServiceInvocationAdvice.OBJECT_MAPPER_ID, ObjectMapper.class);
        if (json != null) {
            if (caller.getParameterCount() == 1) {
                Class<?> type = caller.getParameterTypes()[0];
                args[0] = type.isAssignableFrom(String.class) ? json : mapper.readValue(json, type);
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
        return new Invoker(instance, caller, args, mapper);
    }

    private static class Invoker {
        public final Object instance;
        public final Method caller;
        public final Object[] args;
        public final ObjectMapper mapper;

        public Invoker(Object instance, Method caller, Object[] args, ObjectMapper mapper) {
            this.instance = instance;
            this.caller = caller;
            this.args = args;
            this.mapper = mapper;
        }

        Object invoke() throws Exception {
            return caller.invoke(instance, args);
        }
    }

    private static String errorJSON(ObjectMapper mapper, Throwable ex) {
        if (ex instanceof InvocationTargetException) {
            ex = ((InvocationTargetException) ex).getTargetException();
        }
        var message = ex.getMessage();
        var type = ex instanceof JsonParseException ? ExceptionType.BAD_REQUEST : ExceptionType.SYSTEM;
        Object body = null;
        if (ex instanceof RuleServiceWrapperException) {
            var e = (RuleServiceWrapperException) ex;
            type = e.getType();
            body = e.getBody();
        }
        if (body == null) {
            var b = new LinkedHashMap<String, String>();
            b.put("message", message);
            b.put("type", type.name());
            body = b;
        }

        if (mapper != null) try {
            var x = mapper.createObjectNode();
            x.putNull("result");
            x.putPOJO("error", body);
            return mapper.writeValueAsString(x);
        } catch (Exception ignore) {
        }
        /*
        {"result":null,"error":{"message":"@","type":"$"}}
         */
        return "{\"result\":null,\"error\":{\"message\":\"" + message.replace("\\", "\\\\").replace("\"", "\\\"") +
                "\",\"type\":\"" + type + "\"}}";
    }

    /**
     * Invokes an OpenL method of a deployed service with parameters are presented as an array of JSON strings. It
     * finds the method by the count of the arguments, so the names of arguments does not have matter.
     *
     * @param serviceName Name of deployed service.
     * @param ruleName    Method name to execute, a-ka rule name.
     * @param json        Parameters for method execution as JSON elements.
     * @return Result of execution
     */
    public static String callJSONArgs(String serviceName, String ruleName, String... json) throws Exception {
        var service = getService(serviceName);
        if (service == null) {
            throw new IllegalArgumentException(String.format("Service '%s' is not found.", serviceName));
        }
        var instance = service.getServiceBean();

        int argsCount = json == null ? 0 : json.length;
        ArrayList<Method> methods = new ArrayList<>(2);
        for (Method method : instance.getClass().getMethods()) {
            if (method.getName().equals(ruleName) && method.getParameterCount() == argsCount) {
                methods.add(method);
            }
        }
        if (methods.isEmpty()) {
            throw new IllegalArgumentException(String.format("Method '%s' with %d input arguments is not found in service '%s'.", ruleName, argsCount, serviceName));
        }

        if (methods.size() > 1) {
            throw new IllegalArgumentException(String.format("Non-unique '%s' method name with %d input arguments in service '%s'. There are %d methods with the same name and count of arguments.", ruleName, argsCount, serviceName, methods.size()));
        }

        var caller = methods.get(0);

        var args = new Object[argsCount];

        var mapper = service.getServiceContext().getBean(ServiceInvocationAdvice.OBJECT_MAPPER_ID, ObjectMapper.class);
        for (int i = 0; i < argsCount; i++) {
            Class<?> type = caller.getParameterTypes()[i];
            args[i] = json[i] == null || String.class.isAssignableFrom(type) ? json[i] : mapper.readValue(json[i], type);
        }
        var result = caller.invoke(instance, args);
        return result == null || result instanceof String ? (String) result : mapper.writeValueAsString(result);

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
                (method, args) -> execute(serviceName, method.getName(), method.getParameterTypes(), args),
                proxyInterface
        );
    }

    /**
     * Returns the object instance of the OpenL rules.
     *
     * @param serviceName Name of deployed service.
     * @return the OpenL rules object instance
     */
    public static <T> T get(String serviceName) throws Exception {
        var service = getService(serviceName);
        return service != null ? (T) service.getServiceBean() : null;
    }

    private static org.openl.rules.ruleservice.core.OpenLService getService(String serviceName) {
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
        return rulesFrontend.findServiceByName(serviceName);
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


    private static Object execute(String serviceName,
                                  String ruleName,
                                  Class<?>[] inputParamsTypes,
                                  Object[] params) throws Exception {
        var instance = get(serviceName);
        if (instance == null) {
            throw new IllegalArgumentException(String.format("Service '%s' is not found.", serviceName));
        }
        var method = MethodUtil.getMatchingAccessibleMethod(instance.getClass(), ruleName, inputParamsTypes);
        if (method == null) {
            var types = Arrays.stream(inputParamsTypes).map(x -> x == null ? "null-class" : x.getTypeName()).collect(Collectors.joining(", "));
            throw new IllegalArgumentException(String.format("Method '%s(%s)' is not found in service '%s'.", ruleName, types, serviceName));
        }
        return method.invoke(instance, params);
    }

}

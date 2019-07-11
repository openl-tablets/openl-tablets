package org.openl.rules.ruleservice.kafka.publish;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;

public final class KafkaHelpers {

    private KafkaHelpers() {
    }

    public static String[] getMethodParameters(String methodParameters) {
        String[] parameterTypes = methodParameters.split(",");
        for (int i = 0; i < parameterTypes.length; i++) {
            parameterTypes[i] = parameterTypes[i].trim();
        }
        return parameterTypes;
    }

    public static Method findMethodInService(OpenLService service,
            String methodName,
            String methodParameters) throws RuleServiceInstantiationException {
        int cnt = 0;
        Method method = null;
        for (Method m : service.getServiceClass().getMethods()) {
            if (m.getName().equals(methodName)) {
                method = m;
                cnt++;
            }
        }
        if (cnt == 0) {
            throw new MethodNotFoundException(
                String.format("Method with name '%s' is not found in the service class.", methodName));
        } else if (cnt == 1 && methodParameters == null) {
            return method;
        } else if (cnt > 1 && methodParameters == null) {
            throw new AmbiguousMethodException(
                String.format("Multiple methods with the same name '%s' is found in the service class.", methodName));
        } else {
            String[] methodParametersSplitted = KafkaHelpers.getMethodParameters(methodParameters);
            List<Method> methods = new ArrayList<>();
            for (Method m : service.getServiceClass().getMethods()) {
                if (m.getName().equals(methodName) && isMethodParametersMatched(m.getParameterTypes(),
                    methodParametersSplitted)) {
                    methods.add(m);
                }
            }
            if (methods.isEmpty()) {
                throw new MethodNotFoundException(
                    String.format("Method with name '%s' and parameters '%s' is not found in the service class.",
                        methodName,
                        Arrays.stream(methodParametersSplitted).collect(Collectors.joining(",", "[", "]"))));
            } else {
                if (methods.size() == 1) {
                    return methods.get(0);
                } else {
                    throw new AmbiguousMethodException(String.format(
                        "Multiple methods with the same name '%s' and parameters '%s' is found in the service class.",
                        methodName,
                        Arrays.stream(methodParametersSplitted).collect(Collectors.joining(",", "[", "]"))));
                }
            }
        }
    }

    private static boolean isMethodParametersMatched(Class<?>[] classTypes, String[] types) {
        if (classTypes.length != types.length) {
            return false;
        }
        for (int i = 0; i < classTypes.length; i++) {
            if (!("*".equals(types[i]) || Objects.equals(classTypes[i].getSimpleName(), types[i]) || Objects
                .equals(classTypes[i].getCanonicalName(), types[i]) || Objects.equals(classTypes[i].getName(),
                    types[i]) || Objects.equals(classTypes[i].getTypeName(), types[i]))) {
                return false;
            }
        }
        return true;
    }

}

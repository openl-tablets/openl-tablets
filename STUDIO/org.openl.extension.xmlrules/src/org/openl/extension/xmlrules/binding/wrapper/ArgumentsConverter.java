package org.openl.extension.xmlrules.binding.wrapper;

import java.lang.reflect.Array;

import org.openl.extension.xmlrules.utils.HelperFunctions;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenMethod;

final class ArgumentsConverter {
    private final Class<?>[] paramTypes;

    public ArgumentsConverter(IOpenMethod method) {
        IMethodSignature signature = method.getSignature();

        paramTypes = new Class[signature.getNumberOfParameters()];
        for (int i = 0; i < paramTypes.length; i++) {
            paramTypes[i] = signature.getParameterType(i).getInstanceClass();
        }
    }

    public Object[] convert(Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return arguments;
        }

        Object[] converted = (Object[]) Array.newInstance(arguments.getClass().getComponentType(), arguments.length);
        for (int i = 0; i < arguments.length; i++) {
            converted[i] = HelperFunctions.convertArgument(paramTypes[i], arguments[i]);
        }

        return converted;
    }
}

package org.openl.rules.cmatch.algorithm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

class ArgumentsHelper {
    private final IMethodSignature methodSignature;

    private final Map<String, Argument> argTypes;

    public ArgumentsHelper(IMethodSignature methodSignature) {
        this.methodSignature = methodSignature;

        argTypes = new HashMap<String, Argument>();

        initSimpleArgs();
    }

    private void initSimpleArgs() {
        IOpenClass[] paramTypes = methodSignature.getParameterTypes();
        for (int i = 0; i < methodSignature.getNumberOfArguments(); i++) {
            String name = methodSignature.getParameterName(i);

            IOpenClass type = paramTypes[i];
            if (type.isSimple()) {
                argTypes.put(name, new Argument(i, type));
            } else {
                // non simple will be initialized on demand
            }
        }
    }

    public Argument getTypeByName(String argName) {
        Argument result = argTypes.get(argName);
        if (result != null) {
            return result;
        }

        result = findIndirectByName(argName);
        if (result != null) {
            argTypes.put(argName, result);
        }

        return result;
    }

    private Argument findIndirectByName(String argName) {
        IOpenClass[] paramTypes = methodSignature.getParameterTypes();
        for (int i = 0; i < methodSignature.getNumberOfArguments(); i++) {
            // TODO add source
//            String paramName = methodSignature.getParameterName(i);

            IOpenClass type = paramTypes[i];
            if (type.isSimple()) {
                // ignore, already added
            } else {
                // non simple
                Iterator<IOpenField> fi = type.fields();
                while (fi.hasNext()) {
                    IOpenField field = fi.next();
                    if (argName.equals(field.getName())) {
                        return new Argument(i, field);
                    }
                }
            }
        }

        return null;
    }
}
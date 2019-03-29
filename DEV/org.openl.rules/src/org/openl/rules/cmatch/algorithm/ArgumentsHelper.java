package org.openl.rules.cmatch.algorithm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openl.domain.EnumDomain;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;

public class ArgumentsHelper {
    private final IMethodSignature methodSignature;

    private final Map<String, Argument> argTypes;

    public ArgumentsHelper(IMethodSignature methodSignature) {
        this.methodSignature = methodSignature;

        argTypes = new HashMap<>();

        initSimpleArgs();
    }

    private Argument findIndirectByName(String argName) {
        // snshor: change the way we look up fields in IOpenClass
        // to apply Bex approach
        argName = argName.replace(" ", "");

        IOpenClass[] paramTypes = methodSignature.getParameterTypes();
        for (int i = 0; i < methodSignature.getNumberOfParameters(); i++) {
            // TODO add source
            // String paramName = methodSignature.getParameterName(i);

            IOpenClass type = paramTypes[i];
            if (type.isSimple()) {
                // ignore, already added
            } else {
                IOpenField field = type.getField(argName, false);
                if (field != null) {
                    return new Argument(i, field);
                }
            }
        }

        return null;
    }

    public DomainOpenClass generateDomainClassByArgNames() {
        Set<String> argNames = new HashSet<String>();
        argNames.addAll(argTypes.keySet());

        IOpenClass[] paramTypes = methodSignature.getParameterTypes();
        for (int i = 0; i < methodSignature.getNumberOfParameters(); i++) {
            IOpenClass type = paramTypes[i];
            if (type.isSimple()) {
                // ignore, already added
            } else {
                // non simple
                for (IOpenField field : type.getFields().values()) {
                    argNames.add(field.getName());
                }
            }
        }

        String[] possibleNames = argNames.toArray(new String[argNames.size()]);
        return new DomainOpenClass("names", JavaOpenClass.STRING, new EnumDomain<String>(
                possibleNames), null);
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

    private void initSimpleArgs() {
        IOpenClass[] paramTypes = methodSignature.getParameterTypes();
        for (int i = 0; i < methodSignature.getNumberOfParameters(); i++) {
            String name = methodSignature.getParameterName(i);

            IOpenClass type = paramTypes[i];
            if (type.isSimple()) {
                argTypes.put(name, new Argument(i, type));
            } else {
                // non simple will be initialized on demand
                // except enum(s) that can be referenced in dual mode
                if (type.getInstanceClass().isEnum()) {
                    argTypes.put(name, new Argument(i, type));
                }
            }
        }
    }
}
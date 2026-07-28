package org.openl.rules.cmatch.algorithm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.openl.domain.EnumDomain;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenField;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.StringUtils;

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

        var paramTypes = methodSignature.getParameterTypes();
        for (var i = 0; i < methodSignature.getNumberOfParameters(); i++) {
            // TODO add source
            // String paramName = methodSignature.getParameterName(i);
            var type = paramTypes[i];
            if (!type.isSimple()) {
                var field = type.getField(argName, false);
                if (field != null) {
                    return new Argument(i, field);
                }
            }
        }

        return null;
    }

    public DomainOpenClass generateDomainClassByArgNames() {
        var argNames = new HashSet<String>(argTypes.keySet());

        var paramTypes = methodSignature.getParameterTypes();
        for (var i = 0; i < methodSignature.getNumberOfParameters(); i++) {
            var type = paramTypes[i];
            if (!type.isSimple()) {
                // non simple
                for (IOpenField field : type.getFields()) {
                    argNames.add(field.getName());
                }
            }
        }

        var possibleNames = argNames.toArray(StringUtils.EMPTY_STRING_ARRAY);
        return new DomainOpenClass("names", JavaOpenClass.STRING, new EnumDomain<>(possibleNames), null, null);
    }

    public Argument getTypeByName(String argName) {
        var result = argTypes.get(argName);
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
        var paramTypes = methodSignature.getParameterTypes();
        for (var i = 0; i < methodSignature.getNumberOfParameters(); i++) {
            var name = methodSignature.getParameterName(i);

            var type = paramTypes[i];
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

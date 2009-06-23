package org.openl.rules.liveexcel;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.liveexcel.formula.FunctionParam;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

public class TypeUtils {

    public static IOpenClass getParameterClass(FunctionParam functionParam) {
        Class<?> paramType = functionParam.getParamType();
        try {
            return JavaOpenClass.getOpenClass(paramType);
        } catch (Exception e) {
            return JavaOpenClass.OBJECT;
        }
    }

    public static String convertName(String name) {
        if (name != null) {
            String[] parts = name.split(" ");
            parts[0] = StringUtils.uncapitalize(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                parts[i] = StringUtils.capitalize(parts[i]);
            }
            return StringUtils.join(parts);
        }
        return null;
    }

}

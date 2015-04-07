package org.openl.rules.dt.algorithm;

import java.util.StringTokenizer;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

public class ExpressionTypeUtils {
    public static final String ARRAY_ACCESS_PATTERN = ".+\\[[0-9]+\\]$";

    public static IOpenClass findExpressionType(IOpenClass type, String expression) {
        StringTokenizer stringTokenizer = new StringTokenizer(expression, ".");
        boolean isFirst = true;
        while (stringTokenizer.hasMoreTokens()) {
            String v = stringTokenizer.nextToken();
            boolean arrayAccess = v.matches(ARRAY_ACCESS_PATTERN);
            if (isFirst) {
                if (arrayAccess){
                    type = type.getComponentClass();
                }
                isFirst = false;
                continue;
            }
            IOpenField field = null;
            if (arrayAccess) {
                v = v.substring(0, v.indexOf("["));
            }
            field = type.getField(v);
            type = field.getType();
            if (type.isArray() && arrayAccess) {
                type = type.getComponentClass();
            }
        }
        return type;
    }

    public static String cutExpressionRoot(String expression) {
        StringTokenizer stringTokenizer = new StringTokenizer(expression, ".");
        while (stringTokenizer.hasMoreTokens()) {
            String v = stringTokenizer.nextToken();
            boolean arrayAccess = v.matches(ARRAY_ACCESS_PATTERN);
            if (arrayAccess) {
                v = v.substring(0, v.indexOf("["));
            }
            return v;
        }
        return expression;
    }
}

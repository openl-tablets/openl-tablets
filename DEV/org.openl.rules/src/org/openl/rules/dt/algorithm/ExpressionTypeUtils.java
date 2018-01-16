package org.openl.rules.dt.algorithm;

import java.util.StringTokenizer;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

public class ExpressionTypeUtils {
    /**
     * See also "Using datatype arrays in rules by user defined index" in Reference Guide.
     * Array can be accessed using syntax: drivers[“David”], drivers[“7”].
     * @see org.openl.types.impl.ArrayFieldIndex
     */
    private static final String ARRAY_ACCESS_PATTERN = ".+\\[.+]$";

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
            IOpenField field;
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
        if (stringTokenizer.hasMoreTokens()) {
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

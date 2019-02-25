package org.openl.rules.dt.algorithm;

import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.util.StringUtils;

public class ExpressionTypeUtils {
    /**
     * See also "Using datatype arrays in rules by user defined index" in Reference Guide.
     * Array can be accessed using syntax: drivers[“David”], drivers[“7”].
     * @see org.openl.types.impl.ArrayFieldIndex
     */
    private static final Pattern ARRAY_ACCESS_PATTERN = Pattern.compile(".+\\[.+]$");

    public static IOpenClass findExpressionType(IOpenClass type, String expression) {
        StringTokenizer stringTokenizer = new StringTokenizer(expression, ".");
        boolean isFirst = true;
        while (stringTokenizer.hasMoreTokens()) {
            String v = stringTokenizer.nextToken();
            boolean arrayAccess = StringUtils.matches(ARRAY_ACCESS_PATTERN, v);
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
            boolean arrayAccess = StringUtils.matches(ARRAY_ACCESS_PATTERN, v);
            if (arrayAccess) {
                v = v.substring(0, v.indexOf("["));
            }
            return v;
        }
        return expression;
    }
}

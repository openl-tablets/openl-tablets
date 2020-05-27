package org.openl.rules.table.constraints;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataEnumConstraint extends AbstractConstraint {

    public static final String CONSTRAINT_MATCH = "^\\s*data\\s*:\\s*([\\w_][\\d\\w_]*)\\s*$";
    private static final Object[] NO_PARAMS = new Object[0];

    private Object[] params;

    public DataEnumConstraint(String value) {
        super(value);
    }

    @Override
    public boolean check(Object... valuesToCheck) {
        return false;
    }

    @Override
    public Object[] getParams() {

        if (params == null) {
            params = parseParams();
        }

        return params;
    }

    private Object[] parseParams() {

        // Compile and use regular expression
        Pattern pattern = Pattern.compile(CONSTRAINT_MATCH);
        Matcher matcher = pattern.matcher(getValue());

        boolean matchFound = matcher.find();

        if (matchFound) {
            // Get enumeration name group for this match
            String enumerationName = matcher.group(1);

            return new String[] { enumerationName };
        }

        return NO_PARAMS;
    }
}

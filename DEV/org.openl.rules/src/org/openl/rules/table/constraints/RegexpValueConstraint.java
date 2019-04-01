package org.openl.rules.table.constraints;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Constraint for string values. Checks that the string value matches specified regular expression pattern.
 *
 * @author PUdalau
 */
public class RegexpValueConstraint extends AbstractConstraint {
    public static final String CONSTRAINT_MATCH = "^\\s*regexp\\s*:\\s*(\\S+)\\s*";
    private String regexp;

    public RegexpValueConstraint(String value) {
        super(value);
        regexp = getRegexPattern(value);
    }

    public String getRegexp() {
        return regexp;
    }

    public static String getRegexPattern(String value) {
        Pattern p = Pattern.compile(CONSTRAINT_MATCH);
        Matcher m = p.matcher(value);
        if (m.find()) {
            return m.group(1);
        } else {
            throw new RuntimeException("Incorrect regular expression.");
        }
    }

    @Override
    public Object[] getParams() {
        return new Object[] { String.class };
    }

    @Override
    public boolean check(Object... valuesToCheck) {
        if (valuesToCheck.length == 1 && valuesToCheck[0] instanceof String) {
            return ((String) valuesToCheck[0]).matches(regexp);
        }
        return false;
    }
}

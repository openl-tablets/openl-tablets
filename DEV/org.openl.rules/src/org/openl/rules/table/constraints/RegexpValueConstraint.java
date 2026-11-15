package org.openl.rules.table.constraints;

import java.util.regex.Pattern;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * Constraint for string values. Checks that the string value matches specified regular expression pattern.
 *
 * @author PUdalau
 */
public class RegexpValueConstraint extends AbstractConstraint {
    public static final String CONSTRAINT_MATCH = "^\\s*regexp\\s*:\\s*(\\S+)\\s*";
    @Getter
    private final String regexp;

    public RegexpValueConstraint(String value) {
        super(value);
        regexp = getRegexPattern(value);
    }

    /**
     * The regular expression that the constraints of a table property state.
     *
     * @param constraints the constraints of the property, or {@code null} for none
     * @return the expression of the first regexp constraint, or {@code null} when there is none
     */
    public static @Nullable String regexpOf(@Nullable Constraints constraints) {
        if (constraints == null) {
            return null;
        }
        return constraints.getAll()
                .stream()
                .filter(RegexpValueConstraint.class::isInstance)
                .map(constraint -> ((RegexpValueConstraint) constraint).getRegexp())
                .findFirst()
                .orElse(null);
    }

    public static String getRegexPattern(String value) {
        Pattern p = Pattern.compile(CONSTRAINT_MATCH);
        var m = p.matcher(value);
        if (m.find()) {
            return m.group(1);
        } else {
            throw new IllegalArgumentException("Incorrect regular expression.");
        }
    }

    @Override
    public Object[] getParams() {
        return new Object[]{String.class};
    }

    @Override
    public boolean check(Object... valuesToCheck) {
        if (valuesToCheck.length == 1 && valuesToCheck[0] instanceof String string) {
            return string.matches(regexp);
        }
        return false;
    }
}

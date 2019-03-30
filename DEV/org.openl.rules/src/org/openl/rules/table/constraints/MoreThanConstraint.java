package org.openl.rules.table.constraints;

/**
 * @author Andrei Astrouski
 */
public class MoreThanConstraint extends AbstractConstraint {

    public static final String CONSTRAINT_KEY = ">";

    public static final String CONSTRAINT_MATCH = "^\\s*([_a-zA-Z\\d\\$]+\\s*)?" + CONSTRAINT_KEY + "\\s*[_a-zA-Z\\d\\$]+\\s*$";

    private Object[] params;

    public MoreThanConstraint(String value) {
        super(value);
    }

    private Object[] parseParams() {
        String[] mathes = getValue().replaceAll("\\s", "").split(CONSTRAINT_KEY);
        if (mathes.length > 1) {
            return new String[] { mathes[1] };
        }
        return new String[0];
    }

    @Override
    public Object[] getParams() {
        if (params == null) {
            params = parseParams();
        }
        return params;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean check(Object... valuesToCheck) {
        if (valuesToCheck.length > 1) {
            Object value1 = valuesToCheck[0];
            Object value2 = valuesToCheck[1];
            if (value1 instanceof Comparable && value2 instanceof Comparable) {
                int compareResult = ((Comparable<Object>) value1).compareTo(value2);
                return compareResult > 0;
            }
        }
        return false;
    }

}

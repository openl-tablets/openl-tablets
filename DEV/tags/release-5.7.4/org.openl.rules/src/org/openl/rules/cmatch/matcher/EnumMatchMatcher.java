package org.openl.rules.cmatch.matcher;

public class EnumMatchMatcher implements IMatcher {

    @SuppressWarnings("unchecked")
    private final Class<Enum> enumType;

    @SuppressWarnings("unchecked")
    public EnumMatchMatcher(Class<?> clazz) {
        enumType = (Class<Enum>) clazz;
    }

    @SuppressWarnings("unchecked")
    public Object fromString(String checkValue) {
        return Enum.valueOf(enumType, checkValue);
    }

    public boolean match(Object var, Object checkValue) {
        if (checkValue == null) {
            return false;
        }

        return checkValue.equals(var);
    }
}

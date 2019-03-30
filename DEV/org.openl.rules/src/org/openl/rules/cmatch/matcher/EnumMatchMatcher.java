package org.openl.rules.cmatch.matcher;

public class EnumMatchMatcher implements IMatcher {

    @SuppressWarnings("rawtypes")
    private final Class<Enum> enumType;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public EnumMatchMatcher(Class<?> clazz) {
        enumType = (Class<Enum>) clazz;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object fromString(String checkValue) {
        return Enum.valueOf(enumType, checkValue);
    }

    @Override
    public boolean match(Object var, Object checkValue) {
        if (checkValue == null) {
            return false;
        }

        return checkValue.equals(var);
    }
}

package org.openl.config;

/**
 * @author Aleh Bykhavets
 */
public class ConfigPropertyBoolean extends ConfigProperty<Boolean> {
    // TODO for future use, if needed
    // private static final String[] TRUE_SET = {Boolean.TRUE.toString(), "T",
    // "on", "yes", "Y", "1", "enable", "enabled", "allow"};
    // private static final String[] FALSE_SET = {Boolean.FALSE.toString(), "F",
    // "off", "no", "N", "0", "disable", "disabled", "deny"};

    public ConfigPropertyBoolean(String name, Boolean defValue) {
        super(name, defValue);
    }

    @Override
    protected void setTextValue(String s) {
        if (Boolean.TRUE.toString().equalsIgnoreCase(s)) {
            setValue(Boolean.TRUE);
        } else if (Boolean.FALSE.toString().equalsIgnoreCase(s)) {
            setValue(Boolean.FALSE);
        } else {
            throw new IllegalArgumentException("Not a boolean: '" + s + "'");
        }
    }
}

package org.openl.config;

/**
 * @author Aleh Bykhavets
 */
public class ConfigPropertyLong extends ConfigProperty<Long> {
    public ConfigPropertyLong(String name, Long defValue) {
        super(name, defValue);
    }

    @Override
    protected void setTextValue(String s) {
        setValue(Long.parseLong(s, 10));
    }
}

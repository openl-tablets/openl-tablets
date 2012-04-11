package org.openl.config;

/**
 * @author Aleh Bykhavets
 */
public class ConfigPropertyInteger extends ConfigProperty<Integer> {
    public ConfigPropertyInteger(String name, Integer defValue) {
        super(name, defValue);
    }

    @Override
    protected void setTextValue(String s) {
        setValue(Integer.parseInt(s, 10));
    }
}

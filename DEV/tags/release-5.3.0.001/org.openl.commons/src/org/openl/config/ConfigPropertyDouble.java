package org.openl.config;

/**
 * @author Aleh Bykhavets
 */
public class ConfigPropertyDouble extends ConfigProperty<Double> {
    public ConfigPropertyDouble(String name, Double defValue) {
        super(name, defValue);
    }

    @Override
    protected void setTextValue(String s) {
        setValue(Double.parseDouble(s));
    }
}

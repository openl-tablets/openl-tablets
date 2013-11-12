package org.openl.config;

/**
 * @author Aleh Bykhavets
 */
public abstract class ConfigProperty<T> {
    /** name of config property */
    private String name;
    /** current value of config property */
    private T value;
    /** default value */
    private T defValue;

    public ConfigProperty(String name, T defValue) {
        this.name = name;
        this.defValue = defValue;
    }

    /**
     * Returns default value for the config property.
     * 
     * @return default value
     */
    public T getDefault() {
        return defValue;
    }

    /**
     * Returns name of the config property.
     * 
     * @return name of property
     */
    public String getName() {
        return name;
    }

    /**
     * Returns value of the config property. If <code>value</code> is not
     * defined then <code>default value</code> is returned.
     * 
     * @return value of property
     */
    public T getValue() {
        if (value == null) {
            return defValue;
        }
        return value;
    }

    /**
     * In most cases value is stored as text. Any derived class must implement
     * this method with appropriate code to parse text value into valid config
     * property value. If text value is invalid then
     * {@link IllegalArgumentException} should be thrown.
     * 
     * @param s new value of property in text form
     */
    protected abstract void setTextValue(String s);

    /**
     * Sets value of the config property.
     * 
     * @param value new value
     */
    protected void setValue(T value) {
        this.value = value;
    }
}

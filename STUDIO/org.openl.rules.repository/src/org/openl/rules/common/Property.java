package org.openl.rules.common;

import java.io.Serializable;
import java.util.Date;

public interface Property extends Serializable {
    /**
     * Gets value as Date.
     *
     * @return date value
     * @throws PropertyException if property cannot be transformed into Date
     */
    Date getDate() throws PropertyException;

    /**
     * Gets name of the property.
     *
     * @return name of property
     */
    String getName();

    /**
     * Gets value as String.
     *
     * @return string value
     */
    String getString();

    /**
     * Gets type of value for the property.
     *
     * @return type of value
     */
    ValueType getType();

    /**
     * Gets value of the property as Object.
     *
     * @return value as Object
     */
    Object getValue();

    /**
     * Sets value of the property.
     *
     * @param value date value
     * @throws PropertyException if failed to set new value
     */
    void setValue(Date value) throws PropertyException;

    /**
     * Sets value of the property.
     *
     * @param value string value
     * @throws PropertyException if failed to set new value
     */
    void setValue(String value) throws PropertyException;
}

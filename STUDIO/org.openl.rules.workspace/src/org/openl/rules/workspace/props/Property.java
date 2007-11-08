package org.openl.rules.workspace.props;

import java.util.Date;
import java.io.Serializable;

public interface Property extends Serializable {
    /**
     * Gets name of the property.
     *
     * @return name of property
     */
    String getName();

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
     * Gets value as String.
     *
     * @return string value
     */
    String getString();

    /**
     * Gets value as Date.
     *
     * @return date value
     * @throws PropertyTypeException if property cannot be transformed into Date
     */
    Date getDate() throws PropertyTypeException;

    /**
     * Sets value of the property.
     *
     * @param value string value
     * @throws PropertyTypeException if failed to set new value
     */
    void setValue(String value) throws PropertyTypeException;

    /**
     * Sets value of the property.
     *
     * @param value date value
     * @throws PropertyTypeException if failed to set new value
     */
    void setValue(Date value) throws PropertyTypeException;
}

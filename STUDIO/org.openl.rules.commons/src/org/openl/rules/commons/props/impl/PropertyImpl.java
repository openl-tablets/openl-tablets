package org.openl.rules.commons.props.impl;

import org.openl.rules.commons.props.Property;
import org.openl.rules.commons.props.ValueType;
import org.openl.rules.commons.props.PropertyTypeException;

import java.util.Date;

public class PropertyImpl implements Property {
    private String name;
    private ValueType type;
    private Object value;

    public PropertyImpl(String name, ValueType type, Object value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public PropertyImpl(String name, Date value) {
        this(name, ValueType.DATE, value);
    }

    public PropertyImpl(String name, String value) {
        this(name, ValueType.STRING, value);
    }

    public String getName() {
        return name;
    }

    public ValueType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public String getString() {
        return value.toString();
    }

    public Date getDate() throws PropertyTypeException {
        checkType(ValueType.DATE);
        return (Date)value;
    }

    public void setValue(String value) throws PropertyTypeException {
        checkType(ValueType.STRING);
        this.value = value;
    }

    public void setValue(Date value) throws PropertyTypeException {
        checkType(ValueType.DATE);
        this.value = value;
    }

    protected void checkType(ValueType type) throws PropertyTypeException {
        if (this.type != type) {
            throw new PropertyTypeException("Property has {0} type", this.type);
        }
    }
}

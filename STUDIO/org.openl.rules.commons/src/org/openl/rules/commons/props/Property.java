package org.openl.rules.commons.props;

import java.util.Date;

public interface Property {
    String getName();

    ValueType getType();

    Object getValue();
    String getString();
    Date getDate() throws PropertyTypeException;

    void setValue(String value) throws PropertyTypeException;
    void setValue(Date value) throws PropertyTypeException;
}

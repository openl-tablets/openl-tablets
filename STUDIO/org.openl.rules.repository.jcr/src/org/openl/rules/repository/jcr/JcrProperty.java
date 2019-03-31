package org.openl.rules.repository.jcr;

import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.openl.rules.common.PropertyException;
import org.openl.rules.common.ValueType;
import org.openl.rules.repository.exceptions.RRepositoryException;

public class JcrProperty implements org.openl.rules.common.Property {
    private static final long serialVersionUID = -8880025417798262825L;
    private Node node;
    private String name;
    private ValueType type;
    private Object value;

    protected JcrProperty(Node node, Property p) throws RepositoryException {
        this.node = node;
        name = p.getName();

        Value v = p.getValue();
        switch (v.getType()) {
            case PropertyType.DATE:
                value = v.getDate().getTime();
                type = ValueType.DATE;
                break;
            case PropertyType.BOOLEAN:
                value = v.getBoolean();
                type = ValueType.BOOLEAN;
                break;
            default:
                value = v.getString();
                type = ValueType.STRING;
        }
    }

    protected JcrProperty(Node node, String name, ValueType type, Object value) throws RRepositoryException {
        this.node = node;
        this.name = name;
        this.type = type;
        this.value = value;

        try {
            switch (type) {
                case DATE:
                    Calendar c = date2Calendar((Date) value);
                    node.setProperty(name, c);
                    break;
                case BOOLEAN:
                    node.setProperty(name, (Boolean)value);
                    break;
                default:
                    // STRING
                    node.setProperty(name, value.toString());
            }
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot create property ''{0}''.", e, name);
        }
    }

    private Calendar date2Calendar(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ValueType getType() {
        return type;
    }

    @Override
    public Object getValue() {
        return value;
    }

    // --- private

    public void setValue(Object value) throws RRepositoryException {
        try {
            Property p = node.getProperty(name);

            switch (type) {
                case DATE:
                    Calendar c = date2Calendar((Date) value);

                    p.setValue(c);
                    break;
                default:
                    // STRING
                    p.setValue(value.toString());
            }
            this.value = value;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot set value for ''{0}''.", e, name);
        }
    }

    protected void checkType(ValueType type) throws PropertyException {
        if (this.type != type) {
            throw new PropertyException("Property has {0} type", null, this.type);
        }
    }

    @Override
    public Date getDate() throws PropertyException {
        checkType(ValueType.DATE);
        return (Date) value;
    }

    @Override
    public String getString() {
        return value.toString();
    }
    
    @Override
    public void setValue(Date value) throws PropertyException {
        checkType(ValueType.DATE);
        this.value = value;
    }

    @Override
    public void setValue(String value) throws PropertyException {
        checkType(ValueType.STRING);
        this.value = value;
    }
}

package org.openl.rules.repository.jcr;

import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.openl.rules.repository.RProperty;
import org.openl.rules.repository.RPropertyType;
import org.openl.rules.repository.exceptions.RRepositoryException;

public class JcrProperty implements RProperty {
    private JcrEntity entity;
    private String name;
    private RPropertyType type;
    private Object value;

    protected JcrProperty(JcrEntity entity, Property p) throws RepositoryException {
        this.entity = entity;
        name = p.getName();

        Value v = p.getValue();
        switch (v.getType()) {
            case PropertyType.DATE:
                value = v.getDate().getTime();
                type = RPropertyType.DATE;
                break;
            default:
                value = v.getString();
                type = RPropertyType.STRING;
        }
    }

    protected JcrProperty(JcrEntity entity, String name, RPropertyType type, Object value) throws RRepositoryException {
        this.entity = entity;
        this.name = name;
        this.type = type;
        this.value = value;

        try {
            Node n = entity.node();
            switch (type) {
                case DATE:
                    Calendar c = date2Calendar((Date) value);
                    n.setProperty(name, c);
                    break;
                default:
                    // STRING
                    n.setProperty(name, value.toString());
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

    public String getName() {
        return name;
    }

    public RPropertyType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    // --- private

    public void setValue(Object value) throws RRepositoryException {
        if (value == null) {
            entity.removeProperty(name);
            return;
        }

        try {
            Node n = entity.node();
            Property p = n.getProperty(name);

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
}

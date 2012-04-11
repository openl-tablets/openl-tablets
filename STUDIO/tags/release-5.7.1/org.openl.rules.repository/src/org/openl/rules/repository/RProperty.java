package org.openl.rules.repository;

import org.openl.rules.repository.exceptions.RRepositoryException;

public interface RProperty {
    public String getName();

    public RPropertyType getType();

    public Object getValue();

    public void setValue(Object value) throws RRepositoryException;
}

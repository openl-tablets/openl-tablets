package org.openl.rules.repository;

import org.openl.rules.repository.exceptions.RRepositoryException;

public interface RProperty {
    public String getName();
    public Object getValue();
    public RPropertyType getType();
    public void setValue(Object value)  throws RRepositoryException;
}

package org.openl.rules.repository;

import org.openl.rules.repository.exceptions.RRepositoryException;

public interface RProperty {

    String getName();

    RPropertyType getType();

    Object getValue();

    void setValue(Object value) throws RRepositoryException;

}

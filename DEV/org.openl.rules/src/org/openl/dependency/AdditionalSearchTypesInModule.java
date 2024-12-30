package org.openl.dependency;

import org.openl.types.IOpenClass;

public interface AdditionalSearchTypesInModule {
    IOpenClass getType(String name, IOpenClass module);
}

package org.openl.rules.ui;

import org.openl.util.IdObjectMap;

import java.util.Collection;

public class ObjectMap extends IdObjectMap {
    public Collection<?> getValues() {
        return idObjMap.values();
    }
}

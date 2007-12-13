package org.openl.rules.ui;

import org.openl.util.IdObjectMap;

import java.util.Collection;


/**
 * DOCUMENT ME!
 *
 * @author Aliaksandr Antonik
 */
public class ObjectMap extends IdObjectMap {
    public Collection<?> getValues() {
        return idObjMap.values();
    }
}

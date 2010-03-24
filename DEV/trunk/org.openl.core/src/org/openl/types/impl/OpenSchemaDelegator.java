/*
 * Created on Aug 4, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import java.util.Iterator;

import org.openl.binding.exception.AmbiguousTypeException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenFactory;
import org.openl.types.IOpenSchema;

/**
 * @author snshor
 *
 */
public class OpenSchemaDelegator implements IOpenSchema {
    IOpenSchema delegate;

    public OpenSchemaDelegator(IOpenSchema delegate) {
        this.delegate = delegate;
    }

    /**
     * @return
     */
    public IOpenFactory getFactory() {
        return delegate.getFactory();
    }

    /**
     * @param typename
     * @return
     * @throws AmbiguousTypeException
     */
    public IOpenClass getType(String typename) throws AmbiguousTypeException {
        return delegate.getType(typename);
    }

    /**
     * @return
     */
    public Iterator<String> typeNames() {
        return delegate.typeNames();
    }

}

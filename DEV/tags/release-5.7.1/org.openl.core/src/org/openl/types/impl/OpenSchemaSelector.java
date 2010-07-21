/*
 * Created on Aug 4, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import java.util.Iterator;

import org.openl.binding.exception.AmbiguousTypeException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenSchema;
import org.openl.util.AOpenIterator;
import org.openl.util.ISelector;

/**
 * @author snshor
 *
 */
public class OpenSchemaSelector extends OpenSchemaDelegator {

    ISelector<String> selector;

    /**
     * @param delegate
     */
    public OpenSchemaSelector(IOpenSchema delegate) {
        super(delegate);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.ITypeLibrary#getType(java.lang.String)
     */
    @Override
    public IOpenClass getType(String typename) throws AmbiguousTypeException {
        if (!selector.select(typename)) {
            return null;
        }
        return super.getType(typename);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.ITypeLibrary#types()
     */
    @Override
    public Iterator<String> typeNames() {
        return AOpenIterator.select(super.typeNames(), selector);
    }

}

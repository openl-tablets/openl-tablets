/*
 * Created on Jun 16, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types;

import java.util.Iterator;

import org.openl.binding.exception.AmbiguousTypeException;

/**
 * @author snshor
 *
 */
public interface ITypeLibrary {

    public IOpenClass getType(String typename) throws AmbiguousTypeException;

    /**
     * This method returns an Iterator over all the OpenClasses available in the
     * Schema. Usually this method will be used in edit mode, not in run-time.
     * But even in edit mode we want to avoid loading all the classes,
     * especially if we talk about Java classes, so we provide Iterator of
     * Strings, not the actual IOpenClasses.
     *
     * @see IOpenClassHolder
     * @see IOpenClass
     * @return Iterator of Strings, not IOpenClasses.
     */

    public Iterator<String> typeNames();

}

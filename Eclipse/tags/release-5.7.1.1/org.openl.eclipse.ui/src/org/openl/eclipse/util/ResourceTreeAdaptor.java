/*
 * Created on Aug 28, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.util;

import java.util.Collections;
import java.util.Iterator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.openl.util.OpenIterator;
import org.openl.util.tree.TreeIterator;

/**
 * @author sam
 *
 */
public class ResourceTreeAdaptor extends UtilBase implements TreeIterator.TreeAdaptor {
    public Iterator children(Object node) {
        Iterator result = Collections.EMPTY_LIST.iterator();

        try {
            IResource resource = getResourceAdapter(node);

            if (resource == null) {
                throw new IllegalArgumentException("ResourceTreeAdaptor: Non resource node: " + node);
            }

            if (resource instanceof IContainer) {
                result = OpenIterator.fromArray(((IContainer) resource).members());
            }
        } catch (Throwable t) {
            handleException(t);
        }

        return result;
    }

}

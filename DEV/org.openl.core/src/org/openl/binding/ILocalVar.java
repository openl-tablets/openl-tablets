/*
 * Created on May 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import org.openl.types.IOpenField;

/**
 * @author snshor
 *
 */
public interface ILocalVar extends IOpenField {

    /**
     * Each local variable is put in it's own spot in a stack frame. This method returns an index in the frame.
     *
     * @return
     */
    int getIndexInLocalFrame();

    String getNamespace();
}

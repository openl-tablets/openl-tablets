/*
 * Created on Sep 24, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.launch;

import org.openl.util.IOpenIterator;

/**
 *
 * @author sam
 */
public interface ILaunchRequest {
    /**
     * Returns the launche mode of this request. Example, "run", "debug".
     */
    public String getLaunchMode();

    /**
     * Returns the selection of this request.
     */
    public IOpenIterator getSelection();

    /**
     * Returns <code>selection-is-from-active-editor</code>.
     */
    public boolean isFromEditor();

}

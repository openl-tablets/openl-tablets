/*
 * Created on Sep 18, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.launch;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.openl.eclipse.util.ResourceTreeAdaptor;
import org.openl.util.AOpenIterator;
import org.openl.util.IOpenIterator;
import org.openl.util.OpenIterator;
import org.openl.util.tree.TreeIterator;

/**
 *
 * @author sam
 */
public class LaunchRequest extends LaunchBase implements ILaunchRequest {
    protected Object[] selection;

    protected String mode;

    public LaunchRequest(IEditorPart editor, String mode) {
        selection = new Object[] { editor };
        this.mode = mode;
    }

    public LaunchRequest(IStructuredSelection selection, String mode) {
        this.selection = selection.toArray();
        this.mode = mode;
    }

    public String getLaunchMode() {
        return mode;
    }

    public IOpenIterator getSelection() {
        IOpenIterator result = AOpenIterator.EMPTY;

        for (int i = 0; i < selection.length; i++) {
            IResource resource = getResourceAdapter(selection[i]);
            if (resource != null) {
                result = result.append(new TreeIterator(resource, new ResourceTreeAdaptor(), TreeIterator.DEFAULT));
            }
        }

        return result;
    }

    public boolean isFromEditor() {
        return selection.length > 0 && selection[0] instanceof IEditorPart;
    }

}

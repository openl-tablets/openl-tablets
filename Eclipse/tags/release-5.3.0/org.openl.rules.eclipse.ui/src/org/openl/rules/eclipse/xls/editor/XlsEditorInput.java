/*
 * Created on Dec 23, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.rules.eclipse.xls.editor;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 *
 * @author sam
 */
public class XlsEditorInput extends PlatformObject implements IEditorInput {

    String name;
    String toolTipText;

    public XlsEditorInput(String name, String toolTipText) {
        this.name = name;
        this.toolTipText = toolTipText;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IEditorInput#exists()
     */
    public boolean exists() {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
     */
    public ImageDescriptor getImageDescriptor() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IEditorInput#getName()
     */
    public String getName() {
        // TODO Auto-generated method stub
        return name;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IEditorInput#getPersistable()
     */
    public IPersistableElement getPersistable() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IEditorInput#getToolTipText()
     */
    public String getToolTipText() {
        // TODO Auto-generated method stub
        return toolTipText;
    }

}

package org.openl.rules.ui.repository.beans;

import org.openl.rules.ui.repository.handlers.FolderHandler;

import java.util.List;

/**
 * UI Bean for Folder.
 * 
 * @author Aleh Bykhavets
 *
 */
public class FolderBean extends AbstractEntityBean {
    private List<AbstractEntityBean> elements;
    
    /** {@inheritDoc} */
    public List<AbstractEntityBean> getElements() {
        if (elements == null) {
            FolderHandler folderHandler = (FolderHandler) getHandler();
            elements = folderHandler.getElements(this);
        }
        
        return elements;
    }
}

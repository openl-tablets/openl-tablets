package org.openl.rules.ui.repository.beans;

import org.openl.rules.ui.repository.handlers.FolderHandler;

import java.util.List;

public class FolderBean extends AbstractEntityBean {

    public List<AbstractEntityBean> getElements() {
        FolderHandler folderHandler = (FolderHandler) getHandler();
        return folderHandler.getElements(this);
    }
}

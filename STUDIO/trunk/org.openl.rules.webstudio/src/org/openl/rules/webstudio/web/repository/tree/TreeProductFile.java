package org.openl.rules.webstudio.web.repository.tree;

import org.openl.rules.webstudio.web.repository.UiConst;

public class TreeProductFile extends TreeFile {

    public TreeProductFile(String id, String name) {
        super(id, name);
    }
    
    @Override
    public String getType() {
        return UiConst.TYPE_PRODUCTION_FILE;
    }
}

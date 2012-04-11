package org.openl.rules.ui.tree;

import org.openl.rules.ui.OpenMethodGroupsDictionary;

/**
 * Base tree node builder that uses information about method groups in build
 * process.
 * 
 */
public abstract class OpenMethodsGroupTreeNodeBuilder extends BaseTableTreeNodeBuilder {

    /**
     * Method groups dictionary.
     */
    private OpenMethodGroupsDictionary openMethodGroupsDictionary;

    public void setOpenMethodGroupsDictionary(OpenMethodGroupsDictionary openMethodGroupsDictionary) {
        this.openMethodGroupsDictionary = openMethodGroupsDictionary;
    }

    public OpenMethodGroupsDictionary getOpenMethodGroupsDictionary() {
        return openMethodGroupsDictionary;
    }
}

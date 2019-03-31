package org.openl.rules.ui.tree;

import org.openl.rules.lang.xls.OverloadedMethodsDictionary;

/**
 * Base tree node builder that uses information about method groups in build process.
 * 
 */
public abstract class OpenMethodsGroupTreeNodeBuilder extends BaseTableTreeNodeBuilder {

    /**
     * Method groups dictionary.
     */
    private OverloadedMethodsDictionary openMethodGroupsDictionary;

    public void setOpenMethodGroupsDictionary(OverloadedMethodsDictionary openMethodGroupsDictionary) {
        this.openMethodGroupsDictionary = openMethodGroupsDictionary;
    }

    public OverloadedMethodsDictionary getOpenMethodGroupsDictionary() {
        return openMethodGroupsDictionary;
    }
}

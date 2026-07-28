package org.openl.rules.ui.tree;

import lombok.Getter;
import lombok.Setter;

import org.openl.rules.lang.xls.OverloadedMethodsDictionary;

/**
 * Base tree node builder that uses information about method groups in build process.
 */
public abstract class OpenMethodsGroupTreeNodeBuilder extends BaseTableTreeNodeBuilder {

    /**
     * Method groups dictionary.
     */
    @Getter
    @Setter
    private OverloadedMethodsDictionary openMethodGroupsDictionary;
}

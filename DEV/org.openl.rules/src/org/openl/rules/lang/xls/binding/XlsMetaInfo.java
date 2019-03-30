package org.openl.rules.lang.xls.binding;

import org.openl.meta.IMetaInfo;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;

public class XlsMetaInfo implements IMetaInfo {
    XlsModuleSyntaxNode xlsModuleNode;

    public XlsMetaInfo(XlsModuleSyntaxNode xlsModuleNode) {
        this.xlsModuleNode = xlsModuleNode;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.meta.IMetaInfo#getDisplayName(int)
     */
    public String getDisplayName(int mode) {
        return null;
    }

    public String getSourceUrl() {
        return xlsModuleNode.getModule().getUri();
    }

    public XlsModuleSyntaxNode getXlsModuleNode() {
        return xlsModuleNode;
    }

    public void setXlsModuleNode(XlsModuleSyntaxNode xlsModuleNode) {
        this.xlsModuleNode = xlsModuleNode;
    }

}

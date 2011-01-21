package org.openl.rules.extension.bind;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.syntax.impl.IdentifierNode;

public interface IExtensionBinder {
    
    String getNodeType();
    
    void bind(XlsModuleOpenClass module, XlsModuleSyntaxNode moduleNode, IdentifierNode extension);

}

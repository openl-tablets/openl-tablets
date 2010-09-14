package org.openl.rules.extension.bind;

import org.openl.syntax.impl.IdentifierNode;

public interface IBinderFactory {
    
    IExtensionBinder getNodeBinder(IdentifierNode identifierNode);

}

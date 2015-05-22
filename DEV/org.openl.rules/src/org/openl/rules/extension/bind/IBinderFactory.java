package org.openl.rules.extension.bind;

import org.openl.syntax.impl.IdentifierNode;

/**
 * @deprecated Will be deleted soon. Now extension is declared in rules.xml.
 */
@Deprecated
public interface IBinderFactory {
    
    IExtensionBinder getNodeBinder(IdentifierNode identifierNode);

}

package org.openl.rules.extension.bind;

import org.apache.commons.lang.StringUtils;
import org.openl.syntax.impl.IdentifierNode;

public class NameConventionBinderFactory implements IBinderFactory {

    public static IBinderFactory INSTANCE = new NameConventionBinderFactory();

    public IExtensionBinder getNodeBinder(IdentifierNode identifierNode) {
        String className = "org.openl.rules." + identifierNode.getType() + "."
        + StringUtils.capitalize(identifierNode.getType()) + "Binder";
        try {
            return (IExtensionBinder) Class.forName(className).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Can't create binder: " + className);
        }
    }

}

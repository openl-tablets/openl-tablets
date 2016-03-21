package org.openl.extension.xmlrules.binding;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.TypeCastBinder;
import org.openl.syntax.ISyntaxNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlRulesTypeCastBinder extends TypeCastBinder {
    private final Logger log = LoggerFactory.getLogger(XmlRulesTypeCastBinder.class);

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        return super.bind(node, bindingContext);
    }
}

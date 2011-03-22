/*
 * Created on Jul 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.ANodeBinder;
import org.openl.syntax.ISyntaxNode;

/**
 * @author snshor
 *
 */
public class ModuleNodeBinder extends ANodeBinder {

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        // children should all have type IMemberBoundNode
        IBoundNode[] children = bindChildren(node, bindingContext);
        // TODO fix schema, name
        ModuleOpenClass module = new ModuleOpenClass(null, "UndefinedType", bindingContext.getOpenL());
        ModuleBindingContext moduleContext = new ModuleBindingContext(bindingContext, module);

        for (int i = 0; i < children.length; i++) {
            ((IMemberBoundNode) children[i]).addTo(moduleContext.getModule());
        }

        for (int i = 0; i < children.length; i++) {
            ((IMemberBoundNode) children[i]).finalizeBind(moduleContext);
        }

        return new ModuleNode(node, moduleContext.getModule());
    }

}

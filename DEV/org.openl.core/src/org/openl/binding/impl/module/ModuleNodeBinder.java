/*
 * Created on Jul 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import java.util.List;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.ANodeBinder;
import org.openl.binding.impl.BindHelper;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;

/**
 * @author snshor
 *
 */
public class ModuleNodeBinder extends ANodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        // children should all have type IMemberBoundNode
        IBoundNode[] children = bindChildren(node, bindingContext);
        // TODO fix schema, name
        ModuleOpenClass module = new ModuleOpenClass("UndefinedType", bindingContext.getOpenL());
        processErrors(module.getErrors(), node, bindingContext);
        ModuleBindingContext moduleBindingContext = new ModuleBindingContext(bindingContext, module);

        for (IBoundNode child : children) {
            ((IMemberBoundNode) child).addTo(moduleBindingContext.getModule());
        }

        for (IBoundNode child : children) {
            ((IMemberBoundNode) child).finalizeBind(moduleBindingContext);
        }

        return new ModuleNode(node, moduleBindingContext.getModule());
    }

    private void processErrors(List<Exception> errors, ISyntaxNode node, IBindingContext bindingContext) {
        if (errors != null) {
            for (Exception error : errors) {
                if (error instanceof SyntaxNodeException) {
                    bindingContext.addError((SyntaxNodeException) error);
                } else if (error instanceof CompositeSyntaxNodeException) {
                    BindHelper.processError((CompositeSyntaxNodeException) error, bindingContext);
                } else {
                    BindHelper.processError(error, node, bindingContext);
                }
            }
        }
    }

}

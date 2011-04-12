package org.openl.binding.impl;

import java.util.Iterator;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.util.BooleanUtils;
import org.openl.vm.IRuntimeEnv;

/**
 * Binds conditional index for arrays that returns the first matching element like:
 * - arrayOfDrivers[@ age  < 20];
 * - arrayOfDrivers[select all having  gender == "Male"]     
 * 
 * @author PUdalau
 */
public class SelectFirstIndexNodeBinder extends ANodeBinder {

    private static class ConditionalSelectIndexNode extends ABoundNode {

        public ConditionalSelectIndexNode(ISyntaxNode syntaxNode, IBoundNode[] children) {
            super(syntaxNode, children);
        }

        public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
            IBoundNode container = getContainer();
            IBoundNode condition = getChildren()[1];
            IAggregateInfo aggregateInfo = getType().getAggregateInfo();
            Iterator<Object> elementsIterator = aggregateInfo.getIterator(container.evaluate(env));
            while (elementsIterator.hasNext()) {
                Object element = elementsIterator.next();
                env.pushThis(element);
                if (BooleanUtils.toBoolean(condition.evaluate(env))) {
                    env.popThis();
                    return element;
                }
                env.popThis();
            }
            return null;
        }

        private IBoundNode getContainer() {
            return getChildren()[0];
        }

        public IOpenClass getType() {
            IOpenClass type = getContainer().getType();
            return type.getAggregateInfo().getComponentType(type);
        }
    }

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        BindHelper.processError("This node always binds  with target", node, bindingContext);

        return new ErrorBoundNode(node);
    }

    public IBoundNode bindTarget(ISyntaxNode node, IBindingContext bindingContext, IBoundNode targetNode)
            throws Exception {

        if (node.getNumberOfChildren() != 1) {
            BindHelper.processError("Index node must have  exactly 1 subnode", node, bindingContext);

            return new ErrorBoundNode(node);
        }

        IOpenClass containerType = targetNode.getType();
        IAggregateInfo info = containerType.getAggregateInfo();

        IBoundNode[] children = bindChildren(node,
                new TypeBindingContext(bindingContext, info.getComponentType(containerType)));
        return new ConditionalSelectIndexNode(node, new IBoundNode[] { targetNode, children[0] });
    }
}

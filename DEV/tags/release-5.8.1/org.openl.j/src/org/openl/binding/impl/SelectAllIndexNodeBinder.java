package org.openl.binding.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.util.BooleanUtils;
import org.openl.vm.IRuntimeEnv;

/**
 * Binds conditional index for arrays like:
 * - arrayOfDrivers[@ age  < 20];
 * - arrayOfDrivers[select all having  gender == "Male"]     
 * 
 * @author PUdalau
 */
public class SelectAllIndexNodeBinder extends ANodeBinder {

    private static class ConditionalSelectIndexNode extends ABoundNode {

        public ConditionalSelectIndexNode(ISyntaxNode syntaxNode, IBoundNode[] children) {
            super(syntaxNode, children);
        }

        public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
            IBoundNode container = getContainer();
            IBoundNode condition = getChildren()[1];
            IAggregateInfo aggregateInfo = getType().getAggregateInfo();
            Iterator<Object> elementsIterator = aggregateInfo.getIterator(container.evaluate(env));
            List<Object> firedElements = new ArrayList<Object>();
            while (elementsIterator.hasNext()) {
                Object element = elementsIterator.next();
                env.pushThis(element);
                if (BooleanUtils.toBoolean(condition.evaluate(env))) {
                    firedElements.add(element);
                }
                env.popThis();
            }
            Object result = aggregateInfo.makeIndexedAggregate(aggregateInfo.getComponentType(getType()),
                    new int[] { firedElements.size() });
            for (int i = 0; i < firedElements.size(); i++) {
                Array.set(result, i, firedElements.get(i));
            }
            return result;
        }

        private IBoundNode getContainer() {
            return getChildren()[0];
        }

        public IOpenClass getType() {
            return getContainer().getType();
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

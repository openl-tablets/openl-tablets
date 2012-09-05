package org.openl.binding.impl;

import java.util.Iterator;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
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

    private static final String TEMPORARY_VAR_NAME = "selectFirstIndex";

    private static class ConditionalSelectIndexNode extends ABoundNode {
        private ILocalVar tempVar;

        public ConditionalSelectIndexNode(ISyntaxNode syntaxNode, IBoundNode[] children,ILocalVar tempVar) {
            super(syntaxNode, children);
            this.tempVar = tempVar;
        }

        public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
            IBoundNode container = getContainer();
            IBoundNode condition = getChildren()[1];
            IAggregateInfo aggregateInfo = getType().getAggregateInfo();
            Iterator<Object> elementsIterator = aggregateInfo.getIterator(container.evaluate(env));
            while (elementsIterator.hasNext()) {
                Object element = elementsIterator.next();
                tempVar.set(null, element, env);
                if (BooleanUtils.toBoolean(condition.evaluate(env))) {
                    return element;
                }
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

        String varName = BindHelper.getTemporaryVarName(bindingContext, ISyntaxConstants.THIS_NAMESPACE, TEMPORARY_VAR_NAME);
        ILocalVar var = bindingContext.addVar(ISyntaxConstants.THIS_NAMESPACE, varName, info.getComponentType(containerType));

        IBoundNode[] children = bindChildren(node, new TypeBindingContext(bindingContext, var));
        IBoundNode conditionNode = BindHelper.checkConditionBoundNode(children[0], bindingContext);
        return new ConditionalSelectIndexNode(node, new IBoundNode[] { targetNode, conditionNode }, var);
    }
}

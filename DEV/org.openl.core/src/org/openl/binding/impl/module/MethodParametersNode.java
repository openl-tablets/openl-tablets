/*
 * Created on Jul 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.ABoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.impl.MethodSignature;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class MethodParametersNode extends ABoundNode {

    /**
     * @param syntaxNode
     * @param children
     */
    public MethodParametersNode(ISyntaxNode syntaxNode, IBoundNode[] children) {
        super(syntaxNode, children);
    }

    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        throw new UnsupportedOperationException();
    }

    public IMethodSignature getSignature() {
        int len = children.length;

        ParameterDeclaration[] params = new ParameterDeclaration[len];

        for (int i = 0; i < len; i++) {
            params[i] = new ParameterDeclaration(((ParameterNode) children[i]).getType(), ((ParameterNode) children[i])
                    .getName(), IParameterDeclaration.IN);
        }

        return new MethodSignature(params);

    }

    public IOpenClass getType() {
        return NullOpenClass.the;
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isLiteralExpressionParent() {
        // TODO Auto-generated method stub
        return false;
    }

}

/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 * 
 */
public abstract class ABoundNode implements IBoundNode {
    private static final IBoundNode[] EMPTY = new IBoundNode[0];

    protected ISyntaxNode syntaxNode;
    protected IBoundNode[] children;

    protected ABoundNode(ISyntaxNode syntaxNode, IBoundNode... children) {
        this.syntaxNode = syntaxNode;
        this.children = children != null && children.length == 0 ? EMPTY : children;
    }

    @Override
    public void assign(Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Object evaluate(IRuntimeEnv env) {
        try {
            return evaluateRuntime(env);
        } catch (OpenLRuntimeException ore) {
            throw ore;
        } catch (ControlSignal controlSignal) {
            throw controlSignal;
        } catch (Throwable t) {
            throw new OpenLRuntimeException(t, this);
        }
    }

    abstract protected Object evaluateRuntime(IRuntimeEnv env) throws Exception;

    @Override
    public IOpenClass getType() {
        return NullOpenClass.the;
    }

    public Object[] evaluateChildren(IRuntimeEnv env) {
        if (children == null) {
            return null;
        }

        Object[] ch = new Object[children.length];

        for (int i = 0; i < ch.length; i++) {
            ch[i] = children[i].evaluate(env);
        }

        return ch;
    }

    @Override
    public IBoundNode[] getChildren() {
        return children;
    }

    @Override
    public ISyntaxNode getSyntaxNode() {
        return syntaxNode;
    }

    public void setSyntaxNode(ISyntaxNode syntaxNode) {
        this.syntaxNode = syntaxNode;
    }

    @Override
    public IBoundNode getTargetNode() {
        return null;
    }

    @Override
    public boolean isLvalue() {
        return false;
    }

    @Override
    public void updateAssignFieldDependency(BindingDependencies dependencies) {
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
    }

    @Override
    public boolean isStaticTarget() {
        return false;
    }

}

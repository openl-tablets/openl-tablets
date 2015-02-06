/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.binding.IBoundNodeVisitor;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 * 
 */
@SuppressWarnings("deprecation")
public abstract class ABoundNode implements IBoundNode {

    protected ISyntaxNode syntaxNode;
    protected IBoundNode[] children;

    protected ABoundNode(ISyntaxNode syntaxNode, IBoundNode[] children) {
        this.syntaxNode = syntaxNode;
        this.children = children;
    }

    public void assign(Object value, IRuntimeEnv env) throws OpenLRuntimeException {
        throw new UnsupportedOperationException();
    }

    public Object evaluate(IRuntimeEnv env) throws OpenLRuntimeException {
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

    public Object[] evaluateChildren(IRuntimeEnv env) throws OpenLRuntimeException {

        if (children == null) {
            return null;
        }

        Object[] ch = new Object[children.length];

        for (int i = 0; i < ch.length; i++) {
            ch[i] = children[i].evaluate(env);
        }

        return ch;
    }

    public IBoundNode[] getChildren() {
        return children;
    }

    public ISyntaxNode getSyntaxNode() {
        return syntaxNode;
    }

    public void setSyntaxNode(ISyntaxNode syntaxNode) {
        this.syntaxNode = syntaxNode;
    }

    public IBoundNode getTargetNode() {
        return null;
    }

    public boolean isLvalue() {
        return false;
    }

    public void updateAssignFieldDependency(BindingDependencies dependencies) {
    }

    public boolean isLiteralExpression() {
        return isLiteralExpressionParent() && hasAllLiteralExpressionChildren(this);
    }

    private boolean hasAllLiteralExpressionChildren(IBoundNode boundNode) {
        
        IBoundNodeVisitor checkLiteral = new IBoundNodeVisitor() {

            public boolean visit(IBoundNode node) {
                return node == ABoundNode.this || node.isLiteralExpression();
            }
        };

        return boundNode.visit(checkLiteral);
    }

    public boolean isLiteralExpressionParent() {
        return false;
    }

    public void updateDependency(BindingDependencies dependencies) {
    }
    
    // FIXME: Not right implementation of Visitor pattern.
    // http://en.wikipedia.org/wiki/Visitor_pattern
    // Accepter should simply redirect to the visitors method visit.
    // Currently, as there is an inside logic, it doesn`t allow to 
    // create custom visitor for other purposes.
    //
    public boolean visit(IBoundNodeVisitor visitor) {

        if (!visitor.visit(this)) {
            return false;
        }
        
        if (children == null) {
            return true;
        }
        
        for (int i = 0; i < children.length; i++) {
            if (!children[i].visit(visitor)) {
                return false;
            }
        }
        
        return true;
    }

    public boolean isStaticTarget() {
        return false;
    }

}

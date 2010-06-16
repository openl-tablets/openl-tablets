/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 */
public interface IBoundNode {

    void assign(Object value, IRuntimeEnv env) throws OpenLRuntimeException;

    Object evaluate(IRuntimeEnv env) throws OpenLRuntimeException;

    Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException;

    IBoundNode[] getChildren();

    ISyntaxNode getSyntaxNode();

    IBoundNode getTargetNode();

    IOpenClass getType();

    // Lvalue operations
    boolean isLvalue();
    
    /**
     * Static target will accept only static methods; vice-versa is not necessarily true, but should produce at least a warning
     * @return
     */
    boolean isStaticTarget();
    
    /**
     * 
     * @return true if it is a literal constant, or if this is an expression 
     * containing only literal constants, operators and static methods(except void). In general,
     * if node is literal, it must be able to evaluate properly into some value at compile-time.
     * This property will be used for compile-time optimization, or for checking the literal 
     * constraint on some methods. The literal constraint on parameter or method will imply that 
     * this parameter or all parameters of the method are literal expressions. 
     */
    boolean isLiteralExpression();

    void updateAssignFieldDependency(BindingDependencies dependencies);

    void updateDependency(BindingDependencies dependencies);

    boolean visit(IBoundNodeVisitor visitor);

}

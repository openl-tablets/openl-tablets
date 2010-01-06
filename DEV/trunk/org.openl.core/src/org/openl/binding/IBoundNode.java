/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 */
public interface IBoundNode {

    static final IBoundNode[] EMPTY = {};

    public void assign(Object value, IRuntimeEnv env) throws OpenLRuntimeException;

    public Object evaluate(IRuntimeEnv env) throws OpenLRuntimeException;

    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException;

    public IBoundNode[] getChildren();

    public ISyntaxNode getSyntaxNode();

    public IBoundNode getTargetNode();

    // run-time

    // public String evaluate(Object target, Object[] pars, IRuntimeEnv env);

    public IOpenClass getType();

    // Lvalue operations
    public boolean isLvalue();
    
    
    /**
     * Static target will accept only static methods; vice-versa is not necessarily true, but should produce at least a warning
     * @return
     */
    public boolean isStaticTarget();
    
    /**
     * 
     * @return true if it is a literal constant, or if this is an expression 
     * containing only literal constants, operators and static methods(except void). In general,
     * if node is literal, it must be able to evaluate properly into some value at compile-time.
     * This property will be used for compile-time optimization, or for checking the literal 
     * constraint on some methods. The literal constraint on parameter or method will imply that 
     * this parameter or all parameters of the method are literal expressions. 
     */
    public boolean isLiteralExpression();

    /**
     * @param dependencies
     */
    public void updateAssignFieldDependency(BindingDependencies dependencies);

    /**
     * @param dependencies
     */
    public void updateDependency(BindingDependencies dependencies);

    public boolean visit(IBoundNodeVisitor visitor);

    // TODO generate code
    // public void generateCode(Writer writer, CodeGenSchema cgSchema);

}

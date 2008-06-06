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
public interface IBoundNode
{
	
	static final IBoundNode[] EMPTY = {};
	
	public IOpenClass getType();
	public ISyntaxNode getSyntaxNode();
	
	public IBoundNode[] getChildren();
	
	public IBoundNode getTargetNode();
	
	//Lvalue operations
	public boolean isLvalue();
	
	public void assign(Object value, IRuntimeEnv env) throws OpenLRuntimeException;
	
	//run-time
	
//	public String evaluate(Object target, Object[] pars, IRuntimeEnv env);

	public Object evaluate(IRuntimeEnv env) throws OpenLRuntimeException;

	public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException;
	/**
	 * @param dependencies
	 */
	public void updateDependency(BindingDependencies dependencies);
	/**
	 * @param dependencies
	 */
	public void updateAssignFieldDependency(BindingDependencies dependencies);


	public boolean visit(IBoundNodeVisitor visitor);
	
	
	//TODO generate code
//	public void generateCode(Writer writer, CodeGenSchema cgSchema);

}

/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundCode;
import org.openl.binding.ICastFactory;
import org.openl.binding.INameSpacedMethodFactory;
import org.openl.binding.INameSpacedTypeFactory;
import org.openl.binding.INameSpacedVarFactory;
import org.openl.binding.INodeBinderFactory;
import org.openl.syntax.IParsedCode;

/**
 * @author snshor
 *
 */
public interface IOpenBinder
{
	public IBoundCode bind(IParsedCode parsedCode);

	public IBoundCode bind(IParsedCode parsedCode, IBindingContextDelegator delegator);

	public IBindingContext makeBindingContext();

	
	public INameSpacedTypeFactory getTypeFactory();



	/**
	 * @return
	 */
	public INodeBinderFactory getNodeBinderFactory();

	/**
	 * @return
	 */
	public ICastFactory getCastFactory();

	/**
	 * @return
	 */
	public INameSpacedMethodFactory getMethodFactory();

	/**
	 * @return
	 */
	public INameSpacedVarFactory getVarFactory();
	
	
}

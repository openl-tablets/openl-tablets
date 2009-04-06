/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundNode;
import org.openl.binding.ICastFactory;
import org.openl.binding.INameSpacedMethodFactory;
import org.openl.binding.INameSpacedTypeFactory;
import org.openl.binding.INameSpacedVarFactory;
import org.openl.binding.INodeBinder;
import org.openl.binding.INodeBinderFactory;
import org.openl.syntax.IParsedCode;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.SyntaxErrorException;
import org.openl.syntax.impl.SyntaxError;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public class Binder implements IOpenBinder
{

	INodeBinderFactory nodeBinderFactory;
	INameSpacedMethodFactory methodFactory;
	ICastFactory castFactory;
	INameSpacedVarFactory varFactory;
	INameSpacedTypeFactory typeFactory;
	private OpenL openl;

	public Binder(
		INodeBinderFactory nodeBinderFactory,
		INameSpacedMethodFactory methodFactory,
		ICastFactory castFactory,
		INameSpacedVarFactory varFactory,
		INameSpacedTypeFactory typeFactory, OpenL openl)
	{
		this.nodeBinderFactory = nodeBinderFactory;
		this.methodFactory = methodFactory;
		this.castFactory = castFactory;
		this.varFactory = varFactory;
		this.typeFactory = typeFactory;
		this.openl = openl;
	}

	public INameSpacedTypeFactory getTypeFactory()
	{
		return typeFactory;
	}

	public IBindingContext makeBindingContext()
	{
		return new BindingContext(this, JavaOpenClass.VOID, openl);
		
		
	}

	public IBoundCode bind(IParsedCode parsedCode)
	{
		return bind(parsedCode, null);
	}

	/* (non-Javadoc)
	 * @see org.openl.IOpenBinder#bind(org.openl.syntax.IParsedCode)
	 */
	public IBoundCode bind(
		IParsedCode parsedCode,
		IBindingContextDelegator delegator)
	{

		IBindingContext ibc = makeBindingContext();

		if (delegator != null)
		{
			delegator.setTopDelegate(ibc);
			ibc = delegator;
		}

		ISyntaxNode syntaxNode = parsedCode.getTopNode();
		try
		{
			ibc.pushLocalVarContext();

			INodeBinder nodeBinder = ibc.findBinder(syntaxNode);
			if (nodeBinder == null)
			{
				throw new NullPointerException(
					"Binder not found for node " + syntaxNode.getType());
			}
			IBoundNode topnode = nodeBinder.bind(syntaxNode, ibc);

			ibc.popLocalVarContext();

			return new BoundCode(
				parsedCode,
				topnode,
				ibc.getError(),
				ibc.getLocalVarFrameSize());
		}
		catch (SyntaxErrorException see)
		{
			for (int i = 0; i < see.getSyntaxErrors().length; i++)
			{
				ISyntaxError err = see.getSyntaxErrors()[i];
				ibc.addError(err);
			}

			return new BoundCode(
				parsedCode,
				new ErrorBoundNode(syntaxNode),
				ibc.getError(),
				ibc.getLocalVarFrameSize());
		}
		catch(ProblemsWithChildrenError pwce)
		{
			return new BoundCode(
				parsedCode,
				new ErrorBoundNode(syntaxNode),
				ibc.getError(),
				ibc.getLocalVarFrameSize());
		}
		catch (SyntaxError se)
		{
				ibc.addError(se);

			return new BoundCode(
				parsedCode,
				new ErrorBoundNode(syntaxNode),
				ibc.getError(),
				ibc.getLocalVarFrameSize());
		}

		catch (Throwable t)
		{
			ibc.addError(new BoundError(syntaxNode, "", t));
			return new BoundCode(
				parsedCode,
				new ErrorBoundNode(syntaxNode),
				ibc.getError(),
				ibc.getLocalVarFrameSize());
		}
		
		
		
	}

	/**
	 * @return
	 */
	public INodeBinderFactory getNodeBinderFactory()
	{
		return nodeBinderFactory;
	}

	/**
	 * @return
	 */
	public ICastFactory getCastFactory()
	{
		return castFactory;
	}

	/**
	 * @return
	 */
	public INameSpacedMethodFactory getMethodFactory()
	{
		return methodFactory;
	}

	/**
	 * @return
	 */
	public INameSpacedVarFactory getVarFactory()
	{
		return varFactory;
	}

}

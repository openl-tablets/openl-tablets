package org.openl.rules.lang.xls.binding;

import org.openl.OpenL;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.OpenLRuntimeException;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;

public abstract class AMethodBasedNode extends ATableBoundNode 	implements IMemberBoundNode

{

	protected OpenL openl;
	protected IOpenMethodHeader header;

	protected IOpenMethod method;

	protected ModuleOpenClass module;

	/**
	 * @param syntaxNode
	 * @param children
	 */
	public AMethodBasedNode(
		TableSyntaxNode methodNode,
		OpenL openl,
		IOpenMethodHeader header,
		ModuleOpenClass module)
	{
		super(methodNode, IBoundNode.EMPTY);
		this.header = header;
		this.openl = openl;
		this.module = module;
	}

	public void addTo(ModuleOpenClass openClass)
	{
		openClass.addMethod(method = createMethodShell());
		getTableSyntaxNode().setMember(method);
	}

	protected abstract IOpenMethod createMethodShell();

	public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException
	{
		throw new UnsupportedOperationException("Should not be called");
	}

	public IOpenClass getType()
	{
		return header.getType();
	}

}

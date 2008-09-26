/*
 * Created on Oct 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.method.binding;

import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.OpenlTool;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.OpenLRuntimeException;
import org.openl.binding.impl.BoundError;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.openl.GridTableSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.IRuntimeEnv;


/**
 * @author snshor
 *
 */
public class MethodTableBoundNode
	extends ATableBoundNode
	implements IMemberBoundNode
{

	OpenL openl;
	IOpenMethodHeader header;

	TableMethod method;

	ModuleOpenClass module;

	/**
	 * @param syntaxNode
	 * @param children
	 */
	public MethodTableBoundNode(
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

	/* (non-Javadoc)
	 * @see org.openl.binding.IMemberBoundNode#addTo(org.openl.binding.impl.module.ModuleOpenClass)
	 */
	public void addTo(ModuleOpenClass openClass)
	{
		openClass.addMethod(method = new TableMethod(header, null));
		getTableSyntaxNode().setMember(method);
	}

	/* (non-Javadoc)
	 * @see org.openl.binding.IMemberBoundNode#finalizeBind(org.openl.binding.IBindingContext)
	 */
	public void finalizeBind(IBindingContext cxt) throws Exception
	{

		TableSyntaxNode tsn = getTableSyntaxNode();


		ILogicalTable lt = tsn.getTable();
		
		int expectedHeight = tsn.getTableProperties() == null ? 2 : 3;

		if (lt.getLogicalHeight() != expectedHeight)
			throw new BoundError(
				null,
				"Method table must have 2 row cells, and one optional property row: <header> [properties] <body>",
				null,
				new GridTableSourceCodeModule(lt.getGridTable()));

		

		IOpenSourceCodeModule src =
			new GridCellSourceCodeModule(lt.getLogicalRow(expectedHeight-1).getGridTable());


			OpenlTool.compileMethod(src, openl, method, cxt);
			
			
//		method.setMethodBodyBoundNode(methodBody.getMethodBodyBoundNode());		

	}

	/* (non-Javadoc)
	 * @see org.openl.binding.IBoundNode#evaluateRuntime(org.openl.vm.IRuntimeEnv)
	 */
	public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.openl.binding.IBoundNode#getType()
	 */
	public IOpenClass getType()
	{
		return header.getType();
	}

	public void updateDependency(BindingDependencies dependencies)
	{
		method.getMethodBodyBoundNode().updateDependency(dependencies);
	}
	
	
	class TableMethod extends CompositeMethod implements IMemberMetaInfo
	{

		
		public IMemberMetaInfo getInfo()
		{
			return this;
		}
		
		
		/**
		 * @param header
		 * @param methodBodyBoundNode
		 */
		public TableMethod(IOpenMethodHeader header, IBoundMethodNode methodBodyBoundNode)
		{
			super(header, methodBodyBoundNode);
		}

		public ISyntaxNode getSyntaxNode()
		{
			return MethodTableBoundNode.this.getSyntaxNode();
		}

		/* (non-Javadoc)
		 * @see org.openl.types.IMemberMetaInfo#getDependencies()
		 */
		public BindingDependencies getDependencies()
		{
			BindingDependencies bd = new BindingDependencies();
			updateDependency(bd);
			return bd;
		}



		/* (non-Javadoc)
		 * @see org.openl.meta.IMetaInfo#getSourceUrl()
		 */
		public String getSourceUrl()
		{
			return getTableSyntaxNode().getUri();
		}
		
	}
	
}

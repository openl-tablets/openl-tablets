/*
 * Created on Oct 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.dt.binding;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.OpenLRuntimeException;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.dt.DTLoader;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;


/**
 * @author snshor
 *
 */
public class DecisionTableMethodBoundNode
  extends ATableBoundNode
  implements IMemberBoundNode
{
	
	OpenL openl;
	IOpenMethodHeader header;

	DecisionTable method;	
	
	ModuleOpenClass module;	
	

  /**
   * @param syntaxNode
   * @param children
   */
  public DecisionTableMethodBoundNode(TableSyntaxNode dtNode, OpenL openl, IOpenMethodHeader header, ModuleOpenClass module)
  {
    super(dtNode, IBoundNode.EMPTY);
    this.header = header;
    this.openl = openl;
    this.module = module;
  }

  /* (non-Javadoc)
   * @see org.openl.binding.IMemberBoundNode#addTo(org.openl.binding.impl.module.ModuleOpenClass)
   */
  public void addTo(ModuleOpenClass openClass)
  {
		openClass.addMethod(method = DecisionTable.createTable(header));
		getTableSyntaxNode().setMember(method);
  }

  /* (non-Javadoc)
   * @see org.openl.binding.IMemberBoundNode#finalizeBind(org.openl.binding.IBindingContext)
   */
  public void finalizeBind(IBindingContext cxt) throws Exception
  {
  	

		new DTLoader().load(getTableSyntaxNode(), method, openl, module, (IBindingContextDelegator) cxt);		
		

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
  	method.updateDependency(dependencies);
	}

  
  
  
  
}

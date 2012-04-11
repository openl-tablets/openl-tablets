/*
 * Created on Oct 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.rules.lang.xls.syntax;

import org.openl.IOpenSourceCodeModule;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.syntax.impl.TerminalNode;
import org.openl.util.text.ILocation;

/**
 * @author snshor
 *
 */
public class OpenlSyntaxNode extends TerminalNode implements ITableNodeTypes
{

	String openlName;

  /**
   * @param type
   * @param pos
   * @param module
   */
  public OpenlSyntaxNode(
	  String openlName,
    ILocation pos,
    IOpenSourceCodeModule module)
  {
    super(XLS_OPENL, pos, module);
    this.openlName = openlName;
  }

  /**
   * @return
   */
  public String getOpenlName()
  {
    return openlName;
  }

}

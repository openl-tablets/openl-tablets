package org.openl.rules.calc;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.meta.StringValue;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;

public class SSheetBoundNode   extends AMethodBasedNode
		implements IMemberBoundNode
{


	 public SSheetBoundNode(TableSyntaxNode tsn, OpenL openl, IOpenMethodHeader header, ModuleOpenClass module)
	  {
	    super(tsn, openl, header, module);
	  }

	

	public void finalizeBind(IBindingContext cxt) throws Exception {
		
		ILogicalTable tableBody = this.getTableSyntaxNode().getTableBody();
		
		ILogicalTable rowNamesTable = tableBody.getLogicalColumn(0);
		ILogicalTable columnNamesTable = tableBody.getLogicalRow(0);
		
		
		
		
		for (int row = 1; row < rowNamesTable.getLogicalHeight(); row++) 
		{
			addRowNames(row-1, rowNamesTable.getLogicalRow(row));
		}
		
	}



	private void addRowNames(int row, ILogicalTable logicalRow) 
	{
		for (int i = 0; i < logicalRow.getLogicalWidth(); i++) 
		{
			IGridTable nameCell = logicalRow.getLogicalColumn(i).getGridTable();
			String value = nameCell.getStringValue(0, 0);
			if (value != null)
			{	
				String shortName = "srow" + row + "_" + i;
				StringValue sv =  new StringValue( value
					, shortName, null, nameCell.getUri()) ;
				getSpreadsheet().addRowName(row, sv, i);
			}	
			
		}
	}



	@Override
	protected IOpenMethod createMethodShell() {
		return Spreadsheet.createSpreadsheet(header);
	}
	
	public Spreadsheet getSpreadsheet()
	{
		return (Spreadsheet)method;
	}

}

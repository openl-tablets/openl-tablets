package org.openl.rules.calc;

import org.openl.binding.BindingDependencies;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IDynamicObject;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.AMethod;
import org.openl.vm.IRuntimeEnv;

public class Spreadsheet extends AMethod implements IMemberMetaInfo
{
	
	public Spreadsheet(IOpenMethodHeader header, SSheetBoundNode node) {
		super(header);
		this.node = node;
	}
	
	SSheetBoundNode node;	

	static public Spreadsheet createSpreadsheet(IOpenMethodHeader header, SSheetBoundNode node)
	{
		return new Spreadsheet(header, node);
	}

	public Object invoke(Object target, Object[] params, IRuntimeEnv env) {

		return new SpreadsheetResult(this, (IDynamicObject)target, params, env);
	}
	
	
	
	SCell[][] cells;
	
	
	public int width()
	{
		return cells[0].length;
	}

	public int height()
	{
		return cells.length;
	}

	
	
	SpreadsheetType spreadsheetType; 
	


	public SCell[][] getCells() {
		return cells;
	}

	public void setCells(SCell[][] cells) {
		this.cells = cells;
	}

	public SpreadsheetType getSpreadsheetType() {
		return spreadsheetType;
	}

	public void setSpreadsheetType(SpreadsheetType spreadsheetType) {
		this.spreadsheetType = spreadsheetType;
	}

	public BindingDependencies getDependencies() {
		// TODO Auto-generated method stub
		return null;
	}

	public ISyntaxNode getSyntaxNode() {
		return node.getSyntaxNode();
	}

	public String getSourceUrl() {
		return ((TableSyntaxNode)node.getSyntaxNode()).getUri();
	}

	@Override
	public IMemberMetaInfo getInfo() {
		return this;
	}
	
	
	
	
}

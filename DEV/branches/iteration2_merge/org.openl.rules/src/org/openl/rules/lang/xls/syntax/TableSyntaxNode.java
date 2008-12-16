/*
 * Created on Jun 16, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */

package org.openl.rules.lang.xls.syntax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.openl.meta.StringValue;
import org.openl.rules.indexer.IDocumentType;
import org.openl.rules.indexer.IIndexElement;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.binding.TableProperties;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTable;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ASyntaxNode;
import org.openl.types.IOpenMember;


/**
 * @author snshor
 */

public class TableSyntaxNode  extends ASyntaxNode implements IIndexElement
{

	ILogicalTable table;
//	String header;
	
	HeaderSyntaxNode headerNode;
//	PropertySyntaxNode propertyNode;
	
	TableProperties tableProperties;
	
	IOpenMember member;
	
	Map<String, ILogicalTable> subTables = new HashMap<String, ILogicalTable>();
	
	
	ArrayList<ISyntaxError> errors;
	

	public TableSyntaxNode(
	  String type,
	  GridLocation pos,
	  XlsSheetSourceCodeModule module,
	  IGridTable gridtable, HeaderSyntaxNode header)
	{
	  super(type, pos, null, module);
	  this.table = LogicalTable.logicalTable(gridtable);
	  this.headerNode = header;
	}


	/**
	 * @return
	 */
	public HeaderSyntaxNode getHeader()
	{
		return headerNode;
	}

	public StringValue getHeaderLineValue()
	{
		String value = table.getGridTable().getStringValue(0, 0);
		String uri = table.getGridTable().getUri(0, 0);
		return new StringValue(value, value, value, uri);
	}
	
	
	/**
	 * @return
	 */
	public ILogicalTable getTable()
	{
		return table;
	}




	public XlsSheetSourceCodeModule getXlsSheetSourceCodeModule()
	{
		return (XlsSheetSourceCodeModule)getModule();
	}


	public GridLocation getGridLocation()
	{
		return (GridLocation)getLocation();
	}


	public String getUri()
	{
		return table.getGridTable().getUri();
	}


//	public IIndexElement getParent()
//	{
//		return (XlsSheetSourceCodeModule)getModule();
//	}


	public String getCategory()
	{
		return IDocumentType.WORKSHEET_TABLE.getCategory();
	}


	public String getIndexedText()
	{
//		return table.getGridTable().getStringValue(0, 0);
		return null;
	}


	public String getDisplayName()
	{
		return table.getGridTable().getStringValue(0, 0);
	}


	public TableProperties getTableProperties()
	{
		return tableProperties;
	}


	public void setTableProperties(TableProperties properties)
	{
		this.tableProperties = properties;
	}


	public Map<String, ILogicalTable> getSubTables()
	{
		return subTables;
	}

	public void addError(ISyntaxError error)
	{
		if (errors == null)
			errors = new ArrayList<ISyntaxError>();
		errors.add(error);
	}
	
	public ISyntaxError[] getErrors()
	{
		return errors == null ? null : (ISyntaxError[])errors.toArray(ISyntaxError.EMPTY);
	}
	
	public String getProperty(String name)
	{
		return tableProperties == null ? null : tableProperties.getPropertyValue(name);
	}

	public StringValue getPropertyValue(String name)
	{
		if (tableProperties == null || tableProperties.getProperty(name) == null)
			return null;
		return  tableProperties.getProperty(name).getValue();
	}

	public IOpenMember getMember()
	{
		return this.member;
	}


	public void setMember(IOpenMember member)
	{
		this.member = member;
	}


	public ISyntaxNode getChild(int i)
	{
		// TODO Auto-generated method stub
		return null;
	}


	public int getNumberOfChildren()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	
	Object validationResult;


	public Object getValidationResult() {
		return validationResult;
	}


	public void setValidationResult(Object validationResult) {
		this.validationResult = validationResult;
	}


	public ILogicalTable getTableBody() {
		int startRow = tableProperties == null ? 1 : 2;
		
		if (table.getLogicalHeight() <= startRow)
			return null;
		ILogicalTable tableBody = table.rows(startRow);
		return tableBody;
	}
	
}

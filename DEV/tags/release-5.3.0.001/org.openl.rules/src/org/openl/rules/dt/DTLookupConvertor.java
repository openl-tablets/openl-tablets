package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.CompositeTableGrid;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTable;

/**
 * Lookup table is a decision table that is created by transforming lookup
 * tables to create a single-column return value.
 * 
 * The lookup values could appear either left of the lookup table or on top of
 * it.
 * 
 * The values on the left will be called "vertical" and values on top will be
 * called "horizontal".
 * 
 * 
 * The table should have at least one vertical condition column, it can not have
 * the Rule column, it (in theory) might have vertical Actions which will be
 * processed the same way as vertical conditions, it must have one or more
 * Horizontal Conditions, and exactly one (optional in the future release) RET
 * column
 * 
 * The Horizontal Conditions will be marked HC1, HC2 etc. The first HC column
 * will mark the starting column of the lookup matrix
 */

public class DTLookupConvertor {
	
	public static final int 
		HEADER_ROW 	= 0,
		EXPR_ROW	= 1,
		PARAM_ROW 	= 2,
		DISPLAY_ROW = 3;
	
	
	
	public IGridTable convertTable(ILogicalTable table) {
		
		
		
		IGrid grid = table.getGridTable().getGrid();

		ILogicalTable originaltable = LogicalTable.logicalTable(table);
		
		
		ILogicalTable headerRow = originaltable.getLogicalRow(HEADER_ROW);

		int firstLookupColumn = parseAndValidateLookupHeaders(headerRow);
		
		int firstLookupGridColumn = headerRow.getLogicalColumn(firstLookupColumn).getGridTable().getGridColumn(0,0);
		
		// find and validate horizontal condition keys
		
		ILogicalTable tableWithDisplay = originaltable.rows(DISPLAY_ROW);
		
		ILogicalTable displayRow = tableWithDisplay.getLogicalRow(0);
		IGridRegion displayRowRegion = displayRow.getGridTable().getRegion();
		
		
		
		IGridRegion hcHeadersRegion = new GridRegion(displayRowRegion, IGridRegion.LEFT , firstLookupGridColumn);  
		

		ILogicalTable hcHeaderTable = new GridTable(hcHeadersRegion, grid);
		validateHCHeaders(hcHeaderTable);
		
	 
		
		// create CompositeGrid, build header first, then add conditions/lookups
		
		//header
		int rightColumn = retTable.getGridTable().getRegion().getRight();
		
		ILogicalTable origHeaderTable = originaltable.rows(0, DISPLAY_ROW);
		IGridRegion origHeaderRegion = origHeaderTable.getGridTable().getRegion();
		
		
		IGridRegion modifiedHeaderRegion = new GridRegion(origHeaderRegion, IGridRegion.RIGHT, rightColumn);
		
		IGridTable modifiedHeaderTable = new GridTable(modifiedHeaderRegion, grid);
		
		
		//lookups
		
		ILogicalTable valueTable = originaltable.rows(DISPLAY_ROW+1);
		

		IGridRegion lookupValuesRegion = new GridRegion(valueTable.getGridTable().getRegion(), IGridRegion.LEFT, firstLookupGridColumn);

		IGridTable lookupValuesTable = new GridTable(lookupValuesRegion, grid);
		
		
		//vertical condition values
		
		IGridRegion vcValuesRegion = new GridRegion(valueTable.getGridTable().getRegion(), IGridRegion.RIGHT, firstLookupGridColumn-1);
		IGridTable vcValuesTable = new GridTable(vcValuesRegion, grid);
		
		
		//combine all tables together now
		
		int h = lookupValuesTable.getGridHeight();
		int w = lookupValuesTable.getGridWidth();
		
		List<ILogicalTable> vtables = new ArrayList<ILogicalTable>();
		
		for (int col = 0; col < w; col++) {
			List<ILogicalTable> htables = new ArrayList<ILogicalTable>();
			htables.add(vcValuesTable);
			htables.add(makeHCTable(hcHeaderTable, col, h));
			htables.add(lookupValuesTable.getLogicalColumn(col));
			
			vtables.add(new CompositeTableGrid(htables, false).asGridTable());
			
		}
		
		
		
		
		return new CompositeTableGrid(new IGridTable[]{modifiedHeaderTable, new CompositeTableGrid(vtables, true).asGridTable()}, true).asGridTable();
		

	}

	private ILogicalTable makeHCTable(ILogicalTable hcHeaderTable, int col, int h) 
	{
		List<ILogicalTable> list = new ArrayList<ILogicalTable>();
		ILogicalTable lt = hcHeaderTable.getGridTable().getLogicalColumn(col).transpose();
		for (int i = 0; i < h; i++) 
		{
			list.add(lt);
		}
		
		
		return new CompositeTableGrid(list, true).asGridTable();
	}

	private void validateHCHeaders(ILogicalTable hcHeaderTable) 
	{
		assertEQ(hcHeaders.size(), hcHeaderTable.getGridTable().getLogicalHeight(), 
				"The width of the horizontal keys must be equal to the number of the HC headers");
	}

	private void assertEQ(int v1, int v2, String message) {
		if (v1 == v2) return;
		throw new RuntimeException(message);
	}

	private int parseAndValidateLookupHeaders(ILogicalTable headerRow) {
		int ncol = headerRow.getLogicalWidth();

		for (int i = 0; i < ncol; i++) {
			String headerStr = headerRow.getLogicalColumn(i).getGridTable()
					.getCell(0, 0).getStringValue();
			if (headerStr == null)
				continue;
			headerStr = headerStr.toUpperCase();
			if (DTLoader.isValidConditionHeader(headerStr)
					|| DTLoader.isValidCommentHeader(headerStr))
				continue;
			loadHCandRet(headerRow, i);
			return i;
		}

		throw new RuntimeException(
				"Lookup table must have at least one horizontal condition");
	}

	private void loadHCandRet(ILogicalTable rowHeader, int i) {
		int ncol = rowHeader.getLogicalWidth();
		for (; i < ncol; i++) {
			ILogicalTable htable = rowHeader.getLogicalColumn(i);
			String headerStr = htable.getGridTable()
					.getCell(0, 0).getStringValue();
			if (headerStr == null)
				continue;
			headerStr = headerStr.toUpperCase();
			if (isValidHConditionHeader(headerStr)) {
				if (retTable != null)
					throw new RuntimeException("RET column must be the last one");
				hcHeaders.add(htable);
				assertTableWidth(1, htable, "HC");
				continue;
			}
			if (DTLoader.isValidRetHeader(headerStr))
			{	
				if (retTable != null)
					throw new RuntimeException("Lookup Table can have only one RET column");
				assertTableWidth(1, htable, "RET");
				retTable = htable;
				continue;
			}	
			throw new RuntimeException("Lookup Table allow here only HC or RET columns: " + headerStr);
		}
		
		if (hcHeaders.size() == 0)
			throw new RuntimeException("Lookup Table must have at least one Horizontal Condition (HC1)");
		
		if (retTable == null)
			throw new RuntimeException("Lookup Table must have RET column");
		
	}

	private void assertTableWidth(int w, ILogicalTable htable, String type) 
	{
		if (htable.getGridTable().getGridWidth() == w)
			return;
		throw new RuntimeException("Column " + type  + " must have width=" + w);
	}

	public static  boolean isValidHConditionHeader(String headerStr) 
	{
		return headerStr.startsWith("HC") && headerStr.length() > 2 && Character.isDigit(headerStr.charAt(2));
	}

	List<ILogicalTable> hcHeaders = new ArrayList<ILogicalTable>();
	ILogicalTable retTable = null;

}

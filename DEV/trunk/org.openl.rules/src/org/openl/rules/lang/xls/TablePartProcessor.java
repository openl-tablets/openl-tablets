package org.openl.rules.lang.xls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openl.exception.OpenLCompilationException;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;

public class TablePartProcessor {
	
	public void processTablePart(IGridTable table, XlsSheetSourceCodeModule source) throws OpenLCompilationException
	{
		TablePart tpart = new TablePart(table, source);
		parseHeader(tpart);
		addToParts(tpart);
	}

	Map<String, TreeSet<TablePart>> parts = new HashMap<String, TreeSet<TablePart>>();
	
	synchronized private void addToParts(TablePart tpart) {
		String key = tpart.getPartName();
		TreeSet<TablePart> set = parts.get(key);
		if (set == null)
		{
			set = new TreeSet<TablePart>();
			parts.put(key, set);
		}
		
		set.add(tpart);
	}


	Pattern pattern = Pattern.compile("\\w+\\s+(\\w+)\\s+(column|row)\\s+(\\d+)\\s+of\\s+(\\d+)\\s*($)");

	private void parseHeader(TablePart tpart) throws OpenLCompilationException {
		
        GridCellSourceCodeModule src = new GridCellSourceCodeModule(tpart.getTable());
        
        String header = src.getCode();
        
        Matcher m = pattern.matcher(header);
        
        if (!m.matches())
        {
        	String message = "Valid Syntax: TablePart <table_id> <row|column> <npart(1 to total_number_of_parts)> of <total_number_of_parts>";
        	throw new OpenLCompilationException(message);
        }
        
        String tableId = m.group(1);
        String colOrRow = m.group(2);
        String npart = m.group(3);
        String totalParts = m.group(4);
        
        
        tpart.setPartName(tableId);
        tpart.setPart(Integer.parseInt(npart));
        tpart.setSize(Integer.parseInt(totalParts));
        tpart.setVertical(colOrRow.equals("row"));
		
	}
	
	
	/**
	 * 
	 * @return a list of TableParts with tables merged
	 * @throws OpenLCompilationException
	 */
	
	public List<TablePart> mergeAllNodes() throws OpenLCompilationException
	{
		
		List<TablePart> tables = new ArrayList<TablePart>();
		for (TreeSet<TablePart> set : parts.values()) {
			
			TablePart mergedTable = validateAndMerge(set);
			tables.add(mergedTable);
		}
		
		return tables;
	}


	private TablePart validateAndMerge(TreeSet<TablePart> set) throws OpenLCompilationException {
		
		int cnt = 0;
		int n = set.size();
		
		IGridTable[] tables = new IGridTable[n];
		
		boolean vertical = false;
		int dimension = 0;
		TablePart first = null;
		
		for (Iterator<TablePart> it = set.iterator(); it.hasNext(); ) {
			
			TablePart tablePart = it.next();
			
			if (tablePart.getPart() != cnt + 1)
			{
				String message = "TablePart number " + tablePart.getPart() + " is out of order";
				throw new OpenLCompilationException(message);
			}	
			
			if (tablePart.getSize() != n)
			{
				String message = "TablePart number " + tablePart.getPart() + " has wrong number of parts: " + tablePart.getSize();
				throw new OpenLCompilationException(message);
			}	
			
			IGridTable table = tablePart.getTable().getRows(1);
			boolean myVert = tablePart.isVertical(); 
			int myDim = myVert ? table.getWidth() : table.getHeight();
			
			if (cnt == 0)
			{
				first = tablePart;
				vertical = myVert;
				dimension = myDim;
			}
			else
			{
				if (myVert != vertical)
				{
					String message = "TablePart number " + tablePart.getPart() + " must use " + (vertical ? "row" : "column");
					throw new OpenLCompilationException(message);
				}
		
				if (myDim != dimension)
				{
					String message = "TablePart number " + tablePart.getPart() + " has " + (vertical ? "width" : "height") + " = " + myDim  + " instead of " + dimension;
					throw new OpenLCompilationException(message);
					
				}	
				
			}	
			
			
			

			tables[cnt++] = table;
			
		}
		

		CompositeGrid grid = new CompositeGrid(tables, vertical);
		
		IGridTable table = new GridTable(0, 0, grid.getHeight() - 1, grid.getWidth() - 1, grid);
		
		return new TablePart(table,first.source);
	}
	
	
	
	

}

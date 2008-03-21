package demo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openl.rules.table.ILogicalTable;
import org.openl.rules.ui.TableEditorModel;

public class TableTemplateAdaptor {
	ILogicalTable table;

	ILogicalTable headerColumn;

	public TableTemplateAdaptor(ILogicalTable table) {
		super();
		this.table = table;
		initialize();
	}

	Map<String, Integer> headerMap = new HashMap<String, Integer>();

	void initialize() {
		headerColumn = table.columns(0, 1);

		int h = headerColumn.getLogicalHeight();

		for (int i = 0; i < h; i++) {
			String key = headerColumn.getLogicalRow(i).getGridTable()
					.getStringValue(0, 0);
			headerMap.put(key, i);
		}

		model = new TableEditorModel(table.getGridTable());
	}

	public void addRules(Map<String, String>[] rules) {
		for (int i = 0; i < rules.length; i++) {
			addRuleRow(rules[i], i);
		}
	}

	public void addRuleRow(Map<String, String> map, int i) {
		ILogicalTable colOrRow = insertColumnOrRow(i);
		addRuleRow(map, i + 1, colOrRow);
	}

	private ILogicalTable insertColumnOrRow(int i) {

			if (isColumnRules())
			{	
				if (i > 0)
					model.insertColumns(1, i);
				return model.getUpdatedTable().columns(i+1, i+2);
			}	
			else
			{	
				if (i > 0) 
					model.insertRows(1, i);
				return model.getUpdatedTable().rows(i+1, i+2);
				
			}	
		
	}

	public void addRuleRow(Map<String, String> map, int col,
			ILogicalTable valueColumn) {

		for (Iterator<Map.Entry<String, String>> iterator = map.entrySet()
				.iterator(); iterator.hasNext();) {
			Map.Entry<String, String> entry = iterator.next();

			Integer ruleIndex = headerMap.get(entry.getKey());
			if (ruleIndex == null)
				throw new RuntimeException(
						"Condition/Action header not found: " + entry.getKey());

			// ILogicalTable valueCellTable = valueColumn.getLogicalRow(row);

			if (isColumnRules())
				model.setCellValue(ruleIndex, col, entry.getValue());
			else model.setCellValue(col, ruleIndex, entry.getValue());

		}

	}
	
	
	public void save() throws IOException
	{
		model.save();
	}

	boolean isColumnRules() {
		return table.getGridTable().isNormalOrientation();
	}

	TableEditorModel model;

}

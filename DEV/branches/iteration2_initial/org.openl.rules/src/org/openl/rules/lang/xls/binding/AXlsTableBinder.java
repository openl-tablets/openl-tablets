/*
 * Created on Oct 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.binding;

import java.util.ArrayList;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.ANodeBinder;
import org.openl.binding.impl.BoundError;
import org.openl.meta.StringValue;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridTableSourceCodeModule;
import org.openl.syntax.ISyntaxNode;

/**
 * @author snshor
 * 
 */
public abstract class AXlsTableBinder extends ANodeBinder
{

	public abstract IMemberBoundNode preBind(TableSyntaxNode syntaxNode, OpenL openl,
			IBindingContext cxt, XlsModuleOpenClass module) throws Exception;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openl.binding.INodeBinder#bind(org.openl.syntax.ISyntaxNode,
	 *      org.openl.binding.IBindingContext)
	 */
	public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext)
			throws Exception
	{
		// TODO Auto-generated method stub
		return null;
	}

	static final public String PROPERTIES_HEADER = "properties";

	/**
	 * @param propTable
	 */
	public TableProperties loadProperties(ILogicalTable table)
			throws Exception
	{

		if (table.getLogicalHeight() < 2)
			return null;

		ILogicalTable propTable = table.rows(1, 1);

		if (!PROPERTIES_HEADER
				.equals(propTable.getGridTable().getStringValue(0, 0)))
			return null;

		ILogicalTable propValues = propTable.columns(1);

		ArrayList properties = new ArrayList();

		int h = propValues.getLogicalHeight();
		for (int i = 0; i < h; i++)
		{
			ILogicalTable row = propValues.getLogicalRow(i);

			if (row.getLogicalWidth() < 2)
			{
				throw new BoundError(
						null,
						"Property table must have structure: [property_name] [property_value]",
						null, new GridTableSourceCodeModule(row.getGridTable()));
			}

			String propertyName = row.getGridTable().getStringValue(0, 0);
			if (propertyName == null || propertyName.trim().length() == 0)
				continue;
			String propertyValue = row.getLogicalColumn(1).getGridTable()
					.getStringValue(0, 0);
			if (propertyValue == null || propertyValue.trim().length() == 0)
				continue;
			// validateProperty(propertyName, propertyValue, row);

			StringValue key = new StringValue(propertyName, "key", null, row
					.getGridTable().getUri(0, 0));
			StringValue value = new StringValue(propertyValue, "value", null, row
					.getLogicalColumn(1).getGridTable().getUri(0, 0));
			TableProperties.Property p = new TableProperties.Property(key, value);
			properties.add(p);
		}

		return new TableProperties(propValues, (TableProperties.Property[])properties.toArray(new TableProperties.Property[0]));

	}
}



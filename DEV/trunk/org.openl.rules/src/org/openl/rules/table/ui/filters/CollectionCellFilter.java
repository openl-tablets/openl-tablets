package org.openl.rules.table.ui.filters;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.formatters.FormattersManager;

import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.OpenClassHelper;
import org.openl.util.formatters.IFormatter;

public class CollectionCellFilter extends AGridFilter {

	@Override
	public FormattedCell filterFormat(FormattedCell cell) {
		Object cellValue = cell.getObjectValue();
		if (cellValue != null) {
			if (cellValue.getClass().isArray()) {
				Object[] array = (Object[]) cellValue;
				String formattedValue = null;
				if (array.length == 0) {
					formattedValue = OpenClassHelper.displayNameForCollection(JavaOpenClass.getOpenClass(array.getClass()), 
							true);
				} else {
					IFormatter formatter = FormattersManager.getFormatter(array);
					formattedValue = formatter.format(array);
				}
				cell.setFormattedValue(formattedValue);
			}
		}
		return cell;
	}
}

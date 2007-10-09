package org.openl.rules.webstudio.web.tableeditor;

import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.domain.IntRangeDomain;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.ui.TableEditorModel;
import org.openl.types.IOpenClass;

public class CellEditorSelector
{

    ICellEditorFactory factory = new CellEditorFactory();

    public ICellEditor selectEditor(int row, int col, TableEditorModel model)
    {
	CellMetaInfo meta = model.getCellMetaInfo(row, col);

	IOpenClass dataType = meta == null ? null : meta.getDataType();

	if (dataType == null)
	    return factory.makeTextEditor();

	if (dataType.getInstanceClass() == int.class)
	{
	    IDomain domain = dataType.getDomain();

	    if (domain == null)
		return factory.makeIntEditor(Integer.MIN_VALUE,
			Integer.MAX_VALUE);

	    if (domain instanceof IntRangeDomain)
	    {
		IntRangeDomain range = (IntRangeDomain) domain;
		return factory.makeIntEditor(range.getMin(), range.getMax());
	    }

	}

	if (dataType.getInstanceClass() == String.class)
	{
	    IDomain domain = dataType.getDomain();

	    if (domain instanceof EnumDomain)
	    {
		EnumDomain enumDomain = (EnumDomain) domain;
		return factory.makeComboboxEditor((String[]) enumDomain
			.getEnum().getAllObjects());

	    }

	}

	return factory.makeTextEditor();

    }

}

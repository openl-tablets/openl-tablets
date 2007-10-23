package org.openl.rules.webstudio.web.tableeditor;

public class CellEditorFactory implements ICellEditorFactory
{

    public ICellEditor makeComboboxEditor(String[] choices)
    {
	return new ComboBoxCellEditor(choices, choices);
    }

    public ICellEditor makeIntEditor(int min, int max)
    {
	return new IntRangeCellEditor(min, max);
    }

    public ICellEditor makeTextEditor()
    {
	return new TextCellEditor();
    }

    public ICellEditor makeMultilineEditor()
    {
    return new MultilineEditor(); 
    }
    

}

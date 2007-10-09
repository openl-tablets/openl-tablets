package org.openl.rules.webstudio.web.tableeditor;

import org.openl.rules.webstudio.web.tableeditor.TableEditorController.EditorTypeResponse;

public class ComboBoxCellEditor implements ICellEditor
{

    String[] choices;
    
    public ComboBoxCellEditor(String[] choices)
    {
	this.choices = choices;
    }

    public EditorTypeResponse getEditorTypeAndMetadata()
    {
        EditorTypeResponse typeResponse = new EditorTypeResponse(CE_COMBO);
        typeResponse.setParams(choices);
	return typeResponse;
    }

    public ICellEditorServerPart getServerPart()
    {
	return null;
    }

}

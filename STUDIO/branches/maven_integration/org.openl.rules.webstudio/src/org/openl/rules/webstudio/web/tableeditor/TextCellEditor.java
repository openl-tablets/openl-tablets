package org.openl.rules.webstudio.web.tableeditor;

import org.openl.rules.webstudio.web.tableeditor.TableEditorController.EditorTypeResponse;

public class TextCellEditor implements ICellEditor
{

    public EditorTypeResponse getEditorTypeAndMetadata()
    {
        EditorTypeResponse typeResponse = new EditorTypeResponse(CE_TEXT);
        return typeResponse;
    }

    public ICellEditorServerPart getServerPart()
    {
	return null;
    }

}

package org.openl.rules.webstudio.web.tableeditor;

import org.openl.rules.webstudio.web.tableeditor.TableEditorController.EditorTypeResponse;
import org.openl.rules.webstudio.web.tableeditor.TableEditorController.RangeParam;

public class IntRangeCellEditor implements ICellEditor
{

    int min, max;
    
    public IntRangeCellEditor(int min, int max)
    {
	this.min = min;
	this.max = max;
    }
    
    
    public EditorTypeResponse getEditorTypeAndMetadata()
    {
	EditorTypeResponse typeResponse = new EditorTypeResponse(CE_NUMERIC);
        typeResponse.setParams(new RangeParam((long)min, (long)max));
	return typeResponse;
    }

    public ICellEditorServerPart getServerPart()
    {
	return null;
    }

}

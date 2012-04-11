package org.openl.rules.tableeditor.model;

import org.openl.rules.tableeditor.event.TableEditorController.EditorTypeResponse;
import org.openl.rules.tableeditor.event.TableEditorController.RangeParam;

public class IntRangeCellEditor implements ICellEditor {

    int min, max;

    public IntRangeCellEditor(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public EditorTypeResponse getEditorTypeAndMetadata() {
        EditorTypeResponse typeResponse = new EditorTypeResponse(CE_NUMERIC);
        typeResponse.setParams(new RangeParam((long) min, (long) max));
        return typeResponse;
    }

    public ICellEditorServerPart getServerPart() {
        return null;
    }

}

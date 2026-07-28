package org.openl.rules.tableeditor.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import org.openl.rules.tableeditor.event.TableEditorController.EditorTypeResponse;

public class ComboBoxCellEditor implements ICellEditor {

    @Getter
    @Setter(AccessLevel.PROTECTED)
    private String[] choices;

    @Getter
    @Setter(AccessLevel.PROTECTED)
    private String[] displayValues;

    protected ComboBoxCellEditor(String[] displayValues) {
        this.displayValues = displayValues;
    }

    public ComboBoxCellEditor(String[] choices, String[] displayValues) {
        this.choices = choices;
        this.displayValues = displayValues;
    }

    @Override
    public EditorTypeResponse getEditorTypeAndMetadata() {
        var typeResponse = new EditorTypeResponse(CE_COMBO);
        typeResponse.setParams(new ComboBoxParam(choices, displayValues));
        return typeResponse;
    }

    public static class ComboBoxParam {

        @Getter
        @Setter
        private String[] choices;
        @Getter
        @Setter
        private String[] displayValues;

        public ComboBoxParam(String[] choices, String[] displayValues) {
            this.choices = choices;
            this.displayValues = displayValues;
        }
    }

}

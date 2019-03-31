package org.openl.rules.tableeditor.model;

import org.openl.rules.tableeditor.event.TableEditorController.EditorTypeResponse;

public class ComboBoxCellEditor implements ICellEditor {

    private String[] choices;

    private String[] displayValues;

    protected ComboBoxCellEditor(String[] displayValues) {
        this.displayValues = displayValues;
    }

    protected void setChoices(String[] choices) {
        this.choices = choices;
    }

    protected void setDisplayValues(String[] displayValues) {
        this.displayValues = displayValues;
    }

    public String[] getChoices() {
        return choices;
    }

    public String[] getDisplayValues() {
        return displayValues;
    }

    public ComboBoxCellEditor(String[] choices, String[] displayValues) {
        this.choices = choices;
        this.displayValues = displayValues;
    }

    @Override
    public EditorTypeResponse getEditorTypeAndMetadata() {
        EditorTypeResponse typeResponse = new EditorTypeResponse(CE_COMBO);
        typeResponse.setParams(new ComboBoxParam(choices, displayValues));
        return typeResponse;
    }

    public static class ComboBoxParam {

        private String[] choices;
        private String[] displayValues;

        public ComboBoxParam(String[] choices, String[] displayValues) {
            this.choices = choices;
            this.displayValues = displayValues;
        }

        public String[] getChoices() {
            return choices;
        }

        public String[] getDisplayValues() {
            return displayValues;
        }

        public void setChoices(String[] choices) {
            this.choices = choices;
        }

        public void setDisplayValues(String[] displayValues) {
            this.displayValues = displayValues;
        }
    }

}

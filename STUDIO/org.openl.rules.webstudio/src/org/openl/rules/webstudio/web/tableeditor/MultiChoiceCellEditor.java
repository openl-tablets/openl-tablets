package org.openl.rules.webstudio.web.tableeditor;

public class MultiChoiceCellEditor extends ComboBoxCellEditor {
    public MultiChoiceCellEditor(String[] choices) {
        super(choices);
    }

    @Override
    public TableEditorController.EditorTypeResponse getEditorTypeAndMetadata() {
        TableEditorController.EditorTypeResponse typeResponse = new TableEditorController.EditorTypeResponse(CE_MULTICHOICE);
        typeResponse.setParams(new MultiChoiceParam(choices, ","));
        return typeResponse;
    }

    public static class MultiChoiceParam {
        private String[] choices;
        private String separator;

        public MultiChoiceParam(String[] choices, String separator) {
            this.choices = choices;
            this.separator = separator;
        }

        public String[] getChoices() {
            return choices;
        }

        public void setChoices(String[] choices) {
            this.choices = choices;
        }

        public String getSeparator() {
            return separator;
        }

        public void setSeparator(String separator) {
            this.separator = separator;
        }
    }
}

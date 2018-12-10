package org.openl.rules.tableeditor.model;

import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.tableeditor.event.TableEditorController;

public class MultiSelectCellEditor extends ComboBoxCellEditor {
    public static class MultiChoiceParam extends ComboBoxParam {
        private String separator;
        private String separatorEscaper;

        public MultiChoiceParam(String[] choices, String[] displayValues, String separator, String separatorEscaper) {
            
            super(choices, displayValues);
            this.separator = separator;
            this.setSeparatorEscaper(separatorEscaper);
        }

        public String getSeparator() {
            return separator;
        }

        public void setSeparator(String separator) {
            this.separator = separator;
        }

        public void setSeparatorEscaper(String separatorEscaper) {
            this.separatorEscaper = separatorEscaper;
        }

        public String getSeparatorEscaper() {
            return separatorEscaper;
        }
    }

    public MultiSelectCellEditor(String[] choices, String[] displayValues) {
        super(displayValues);
        String[] insertedEscChoices = new String[choices.length];
        for (int i = 0; i < choices.length; i++) {
            insertedEscChoices[i] = choices[i].replaceAll(RuleRowHelper.ARRAY_ELEMENTS_SEPARATOR,
                RuleRowHelper.ARRAY_ELEMENTS_SEPARATOR_ESCAPER + RuleRowHelper.ARRAY_ELEMENTS_SEPARATOR);
        }
        super.setChoices(insertedEscChoices);
    }

    @Override
    public TableEditorController.EditorTypeResponse getEditorTypeAndMetadata() {
        TableEditorController.EditorTypeResponse typeResponse = new TableEditorController.EditorTypeResponse(
                CE_MULTISELECT);
        typeResponse.setParams(new MultiChoiceParam(getChoices(), getDisplayValues(),
            RuleRowHelper.ARRAY_ELEMENTS_SEPARATOR,
            RuleRowHelper.ARRAY_ELEMENTS_SEPARATOR_ESCAPER));
        return typeResponse;
    }
}

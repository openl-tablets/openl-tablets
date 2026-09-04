package org.openl.rules.tableeditor.model;

import lombok.Getter;
import lombok.Setter;

import org.openl.rules.tableeditor.event.TableEditorController;

public class MultiSelectCellEditor extends ComboBoxCellEditor {
    private static final String ARRAY_ELEMENTS_SEPARATOR = ",";
    private static final String ARRAY_ELEMENTS_SEPARATOR_ESCAPER = "\\";

    public static class MultiChoiceParam extends ComboBoxParam {
        @Getter
        @Setter
        private String separator;
        @Getter
        @Setter
        private String separatorEscaper;

        public MultiChoiceParam(String[] choices, String[] displayValues, String separator, String separatorEscaper) {

            super(choices, displayValues);
            this.separator = separator;
            this.setSeparatorEscaper(separatorEscaper);
        }
    }

    public MultiSelectCellEditor(String[] choices, String[] displayValues) {
        super(displayValues);
        String[] insertedEscChoices = new String[choices.length];
        for (var i = 0; i < choices.length; i++) {
            insertedEscChoices[i] = choices[i].replaceAll(ARRAY_ELEMENTS_SEPARATOR,
                    ARRAY_ELEMENTS_SEPARATOR_ESCAPER + ARRAY_ELEMENTS_SEPARATOR);
        }
        super.setChoices(insertedEscChoices);
    }

    @Override
    public TableEditorController.EditorTypeResponse getEditorTypeAndMetadata() {
        var typeResponse = new TableEditorController.EditorTypeResponse(
                CE_MULTISELECT);
        typeResponse.setParams(new MultiChoiceParam(getChoices(),
                getDisplayValues(),
                ARRAY_ELEMENTS_SEPARATOR,
                ARRAY_ELEMENTS_SEPARATOR_ESCAPER));
        return typeResponse;
    }
}

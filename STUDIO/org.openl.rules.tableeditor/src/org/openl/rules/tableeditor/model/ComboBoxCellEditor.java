package org.openl.rules.tableeditor.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ComboBoxCellEditor implements ICellEditor {

    private final String[] choices;
    private final String[] displayValues;

    @Override
    public EditorTypeResponse getEditorTypeAndMetadata() {
        return new EditorTypeResponse(CE_COMBO, new ComboBoxParam(choices, displayValues));
    }

    /**
     * The values one of which is chosen.
     *
     * @param choices       the values to choose from
     * @param displayValues what to show for each choice, in the same order
     */
    public record ComboBoxParam(String[] choices, String[] displayValues) {
    }

}

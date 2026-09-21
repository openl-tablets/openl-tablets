package org.openl.rules.tableeditor.model;

import java.util.Arrays;

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

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ComboBoxParam(var otherChoices, var otherDisplayValues)
                    && Arrays.equals(choices, otherChoices)
                    && Arrays.equals(displayValues, otherDisplayValues);
        }

        @Override
        public int hashCode() {
            return 31 * Arrays.hashCode(choices) + Arrays.hashCode(displayValues);
        }

        @Override
        public String toString() {
            return "ComboBoxParam[choices=%s, displayValues=%s]".formatted(Arrays.toString(choices),
                    Arrays.toString(displayValues));
        }
    }

}

package org.openl.rules.tableeditor.model;

import java.util.Arrays;
import java.util.Objects;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MultiSelectCellEditor implements ICellEditor {

    private static final String ARRAY_ELEMENTS_SEPARATOR = ",";
    private static final String ARRAY_ELEMENTS_SEPARATOR_ESCAPER = "\\";

    private final String[] choices;
    private final String[] displayValues;

    @Override
    public EditorTypeResponse getEditorTypeAndMetadata() {
        return new EditorTypeResponse(CE_MULTISELECT,
                new MultiChoiceParam(choices,
                        displayValues,
                        ARRAY_ELEMENTS_SEPARATOR,
                        ARRAY_ELEMENTS_SEPARATOR_ESCAPER));
    }

    /**
     * The values several of which are chosen, and how the chosen ones are written into one cell.
     *
     * @param choices          the values to choose from
     * @param displayValues    what to show for each choice, in the same order
     * @param separator        what separates the chosen values in the cell
     * @param separatorEscaper what precedes a separator that belongs to a value
     */
    public record MultiChoiceParam(String[] choices, String[] displayValues, String separator,
                                   String separatorEscaper) {

        @Override
        public boolean equals(Object obj) {
            return obj instanceof MultiChoiceParam(
                    var otherChoices, var otherDisplayValues, var otherSeparator, var otherSeparatorEscaper
            ) && Arrays.equals(choices, otherChoices)
                    && Arrays.equals(displayValues, otherDisplayValues)
                    && Objects.equals(separator, otherSeparator)
                    && Objects.equals(separatorEscaper, otherSeparatorEscaper);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(choices), Arrays.hashCode(displayValues), separator,
                    separatorEscaper);
        }

        @Override
        public String toString() {
            return "MultiChoiceParam[choices=%s, displayValues=%s, separator=%s, separatorEscaper=%s]"
                    .formatted(Arrays.toString(choices), Arrays.toString(displayValues), separator,
                            separatorEscaper);
        }
    }

}

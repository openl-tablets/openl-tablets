package org.openl.rules.tableeditor.model;

import org.openl.rules.tableeditor.event.TableEditorController.EditorTypeResponse;
import org.openl.util.RangeWithBounds;

public class NumberRangeEditor implements ICellEditor {

    private String entryEditor;
    private String parsedValue;

    public NumberRangeEditor(String entryEditor, String initialValue) {
        this.entryEditor = entryEditor;
        this.parsedValue = parseValue(initialValue);
    }

    @Override
    public EditorTypeResponse getEditorTypeAndMetadata() {
        NumberRangeParams params = new NumberRangeParams();
        params.setEntryEditor(entryEditor);
        params.setParsedValue(parsedValue);

        EditorTypeResponse typeResponse = new EditorTypeResponse(CE_RANGE);
        typeResponse.setParams(params);
        return typeResponse;
    }

    private String parseValue(String input) {
        if (input == null) {
            return "";
        }
        String min;
        String max;
        RangeWithBounds range;
        if (ICellEditor.CE_INTEGER.equals(entryEditor)) {
            try {
                DetailedIntRangeParser parser = new DetailedIntRangeParser();
                range = parser.parse(input);
                if (range.getMax().equals(Integer.MAX_VALUE)) {
                    StringBuilder builder = new StringBuilder(">");
                    if (range.getLeftBoundType() == RangeWithBounds.BoundType.INCLUDING) {
                        builder.append("=");
                    }
                    builder.append(" ").append(parser.getMin());
                    return builder.toString();
                }
                if (range.getMin().equals(Integer.MIN_VALUE)) {
                    StringBuilder builder = new StringBuilder("<");
                    if (range.getLeftBoundType() == RangeWithBounds.BoundType.INCLUDING) {
                        builder.append("=");
                    }
                    builder.append(" ").append(parser.getMax());
                    return builder.toString();
                }
                if (range.getMax().equals(range.getMin())) {
                    return input;
                }
                min = parser.getMin();
                max = parser.getMax();
            } catch (RuntimeException e) {
                // Range format contains syntax error
                return input;
            }
        } else if (ICellEditor.CE_DOUBLE.equals(entryEditor)) {
            try {
                DetailedDoubleRangeParser parser = new DetailedDoubleRangeParser();
                range = parser.parse(input);
                if (range.getMax().equals(Double.POSITIVE_INFINITY) || range.getMin()
                    .equals(Double.NEGATIVE_INFINITY)) {
                    return input;
                }
                if (range.getMax().equals(range.getMin())) {
                    return input;
                }
                min = parser.getMin();
                max = parser.getMax();
            } catch (Exception e) {
                // Range format contains syntax error
                return input;
            }
        } else {
            throw new UnsupportedOperationException("Unsupported range type");
        }

        StringBuilder builder = new StringBuilder();
        if (range.getLeftBoundType() == RangeWithBounds.BoundType.INCLUDING) {
            builder.append('[');
        } else {
            builder.append('(');
        }
        builder.append(min).append(" .. ").append(max);
        if (range.getRightBoundType() == RangeWithBounds.BoundType.INCLUDING) {
            builder.append(']');
        } else {
            builder.append(')');
        }
        return builder.toString();
    }

    public static class NumberRangeParams {

        private String entryEditor;
        private String parsedValue;

        public String getEntryEditor() {
            return entryEditor;
        }

        public void setEntryEditor(String entryEditor) {
            this.entryEditor = entryEditor;
        }

        public String getParsedValue() {
            return parsedValue;
        }

        public void setParsedValue(String parsedValue) {
            this.parsedValue = parsedValue;
        }
    }

}

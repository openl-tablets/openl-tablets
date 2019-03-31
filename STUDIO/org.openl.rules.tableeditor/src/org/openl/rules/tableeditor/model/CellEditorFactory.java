package org.openl.rules.tableeditor.model;

public class CellEditorFactory implements ICellEditorFactory {

    @Override
    public ICellEditor makeComboboxEditor(String[] choices) {
        return makeComboboxEditor(choices, choices);
    }

    @Override
    public ICellEditor makeComboboxEditor(String[] choices, String[] displayValues) {
        return new ComboBoxCellEditor(choices, displayValues);
    }

    @Override
    public ICellEditor makeNumericEditor(Number min, Number max, boolean intOnly) {
        return new NumericCellEditor(min, max, intOnly);
    }

    @Override
    public ICellEditor makeMultilineEditor() {
        return new MultilineEditor();
    }

    @Override
    public ICellEditor makeTextEditor() {
        return new TextCellEditor();
    }

    @Override
    public ICellEditor makeFormulaEditor() {
        return new FormulaCellEditor();
    }

    @Override
    public ICellEditor makeDateEditor() {
        return new DateCellEditor();
    }

    @Override
    public ICellEditor makeBooleanEditor() {
        return new BooleanCellEditor();
    }

    @Override
    public ICellEditor makeMultiSelectEditor(String[] choices) {
        return makeMultiSelectEditor(choices, choices);
    }

    @Override
    public ICellEditor makeMultiSelectEditor(String[] choices, String[] dispalayValues) {
        return new MultiSelectCellEditor(choices, dispalayValues);
    }

    @Override
    public ICellEditor makeArrayEditor(String separator, String entryEditor, boolean intOnly) {
        return new ArrayCellEditor(separator, entryEditor, intOnly);
    }

    @Override
    public ICellEditor makeNumberRangeEditor(String entryEditor, String initialValue) {
        return new NumberRangeEditor(entryEditor, initialValue);
    }

}

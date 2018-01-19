package org.openl.rules.tableeditor.model;

public class CellEditorFactory implements ICellEditorFactory {

    public ICellEditor makeComboboxEditor(String[] choices) {
        return makeComboboxEditor(choices, choices);
    }
    
    public ICellEditor makeComboboxEditor(String[] choices, String[] displayValues) {
        return new ComboBoxCellEditor(choices, displayValues);
    }

    public ICellEditor makeNumericEditor(Number min, Number max, boolean intOnly) {
        return new NumericCellEditor(min, max, intOnly);
    }

    public ICellEditor makeMultilineEditor() {
        return new MultilineEditor();
    }

    public ICellEditor makeTextEditor() {
        return new TextCellEditor();
    }

    public ICellEditor makeFormulaEditor() {
        return new FormulaCellEditor();
    }
    
    public ICellEditor makeDateEditor() {
        return new DateCellEditor();
    }
    
    public ICellEditor makeBooleanEditor() {
        return new BooleanCellEditor();
    }

    public ICellEditor makeMultiSelectEditor(String[] choices) {        
        return makeMultiSelectEditor(choices, choices);
    }

    public ICellEditor makeMultiSelectEditor(String[] choices, String[] dispalayValues) {        
        return new MultiSelectCellEditor(choices, dispalayValues);
    }

    public ICellEditor makeArrayEditor(String separator, String entryEditor, boolean intOnly) {
        return new ArrayCellEditor(separator, entryEditor, intOnly);
    }
    
    public ICellEditor makeNumberRangeEditor(String entryEditor, String initialValue) {
        return new NumberRangeEditor(entryEditor, initialValue);
    }

}

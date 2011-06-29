package org.openl.rules.tableeditor.model;

public class CellEditorFactory implements ICellEditorFactory {

    public ICellEditor makeComboboxEditor(String[] choices) {
        return makeComboboxEditor(choices, choices);
    }
    
    public ICellEditor makeComboboxEditor(String[] choices, String[] displayValues) {
        return new ComboBoxCellEditor(choices, displayValues);
    }

    public ICellEditor makeNumericEditor(Number min, Number max) {
        return new NumericCellEditor(min, max);
    }

    public ICellEditor makeNumericEditor() {
        return makeNumericEditor(null, null);
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

    public ICellEditor makeArrayEditor() {
        return new ArrayCellEditor();
    }

    public ICellEditor makeArrayEditor(String separator, String entryEditor) {
        return new ArrayCellEditor(separator, entryEditor);
    }

}

package org.openl.rules.webstudio.web.tableeditor;

/**
 * 
 * @author snshor
 * 
 * Provides interface for table editor creator. An implementation can differ in different scenarios
 * It allows to separate an actual implementation from the caller, for example, the call to makeCodeEditor() 
 * may return simple multi-line editor for now 
 */

public interface ICellEditorFactory
{
    /**
     * Creates int range editor with min,max bounds. 
     * @param min
     * @param max
     * @return
     */
  ICellEditor makeIntEditor(int min, int max);
  
  
  /**
   * Creates combobox editor with choices in String array
   * @param choices
   * @return
   */
  ICellEditor makeComboboxEditor(String[] choices);


    ICellEditor makeTextEditor();

    ICellEditor makeMultilineEditor();
}

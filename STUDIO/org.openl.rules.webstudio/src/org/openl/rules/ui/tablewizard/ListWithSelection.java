package org.openl.rules.ui.tablewizard;

import java.util.ArrayList;

/**
 * @author Aliaksandr Antonik.
 */
public class ListWithSelection<T> extends ArrayList<T> {

    private static final long serialVersionUID = 1L;

    private int selectedIndex;

    public T getSelectedElement() {
        return isSelectionValid() ? get(selectedIndex) : null;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public boolean isSelectionValid() {
        return selectedIndex >= 0 && selectedIndex < size();
    }

    @Override
    public T remove(int index) {
        if (index < selectedIndex || index == selectedIndex && selectedIndex == size() - 1) {
            --selectedIndex;
        }
        return super.remove(index);
    }

    public void selectLast() {
        selectedIndex = size() - 1;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }
}

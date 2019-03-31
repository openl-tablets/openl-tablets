package org.openl.rules.ui.util;

/**
 * Created by Andrei Ostrovski on 10.12.13.
 */
public class ListItem<T> {

    private boolean selected;
    private T item;

    public ListItem() {
    }

    public ListItem(boolean selected, T item) {
        this.selected = selected;
        this.item = item;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public T getItem() {
        return item;
    }

    public void setItem(T dependency) {
        this.item = dependency;
    }
}

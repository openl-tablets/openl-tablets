package org.openl.rules.ui.tablewizard;

import java.util.List;
import javax.faces.model.SelectItem;

/**
 * @author Aliaksandr Antonik.
 */
public class OpenLTypes {
    private SelectItem[] items;

    public void setOptions(List<String> options) {
        items = new SelectItem[options.size()];
        int pos = 0;
        for (String s : options) items[pos++] = new SelectItem(s, s);
    }

    public SelectItem[] getItems() {
        return items;
    }
}

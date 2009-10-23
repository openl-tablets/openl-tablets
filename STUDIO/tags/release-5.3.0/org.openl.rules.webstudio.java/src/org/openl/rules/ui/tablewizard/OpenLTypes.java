package org.openl.rules.ui.tablewizard;

import java.util.List;
import javax.faces.model.SelectItem;

/**
 * @author Aliaksandr Antonik.
 */
public class OpenLTypes {
    private SelectItem[] returnTypes;
    private SelectItem[] paramTypes;

    public SelectItem[] getParamTypes() {
        return paramTypes;
    }

    public SelectItem[] getReturnTypes() {
        return returnTypes;
    }

    public void setOptions(List<String> options) {
        returnTypes = new SelectItem[options.size()];
        int pos = 0;
        for (String s : options) {
            returnTypes[pos++] = new SelectItem(s, s);
        }

        options.remove("void");
        paramTypes = new SelectItem[options.size()];
        pos = 0;
        for (String s : options) {
            paramTypes[pos++] = new SelectItem(s, s);
        }
    }
}

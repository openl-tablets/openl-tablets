package org.openl.ie.constrainer;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company:
 * </p>
 *
 * @author unascribed
 * @version 1.0
 */

public class ChoicePointLabel implements java.io.Serializable {
    private int _label;
    private Constrainer _c;

    ChoicePointLabel(Constrainer c, int label) {
        _c = c;
        _label = label;
    }

    public boolean equals(ChoicePointLabel cpl) {
        return _label == cpl._label && _c == cpl._c;
    }

    @Override
    public int hashCode() {
        return _label;
    }
}
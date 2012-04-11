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

public class IntSetVarArray extends ConstrainerObjectImpl {
    IntSetVar[] _data;

    public IntSetVarArray(Constrainer C, int size) {
        this(C, size, "");
    }

    public IntSetVarArray(Constrainer C, int size, String name) {
        super(C, name);
        _data = new IntSetVar[size];
    }

    public IntSetVar get(int idx) {
        return _data[idx];
    }

    public void set(IntSetVar var, int idx) {
        _data[idx] = var;
    }

    public int size() {
        return _data.length;
    }
}
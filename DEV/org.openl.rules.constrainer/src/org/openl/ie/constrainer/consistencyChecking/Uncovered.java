package org.openl.ie.constrainer.consistencyChecking;

import java.util.HashMap;

import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.consistencyChecking.DTChecker.Utils;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description: Representation of a point in the space of states not covered by any rule
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
public class Uncovered {
    protected String[] _solutionNames = null;
    protected int[] _solutionValues = null;

    public Uncovered(IntExpArray array) {
        _solutionNames = Utils.IntExpArray2Names(array);
        _solutionValues = Utils.IntExpArray2Values(array);
    }

    public HashMap getSolution() {
        HashMap map = new HashMap();
        for (int i = 0; i < _solutionNames.length; i++) {
            map.put(_solutionNames[i], new Integer(_solutionValues[i]));
        }
        return map;
    }

    public String[] getSolutionNames() {
        return _solutionNames;
    }

    public int[] getSolutionValues() {
        return _solutionValues;
    }

    @Override
    public String toString() {
        return getSolution().toString();
    }

}
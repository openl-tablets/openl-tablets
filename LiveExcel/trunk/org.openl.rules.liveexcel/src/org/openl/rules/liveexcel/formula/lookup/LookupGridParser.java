package org.openl.rules.liveexcel.formula.lookup;

import org.apache.poi.hssf.record.formula.eval.AreaEval;
import org.apache.poi.hssf.record.formula.eval.BlankEval;
import org.apache.poi.hssf.record.formula.eval.StringValueEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;

/**
 * Parses declared lookups(OL_DECLARE_TABLE). Creates linearized {@link Grid}
 * for lookup.
 * 
 * @author PUdalau
 */
public class LookupGridParser {
    private AreaEval lookupArea;

    /**
     * @return Working area of lookup.
     */
    public AreaEval getLookupArea() {
        return lookupArea;
    }

    /**
     * Creates LookupGridParser by area with lookup data.
     * 
     * @param lookupArea Working area of lookup.
     */
    public LookupGridParser(AreaEval lookupArea) {
        this.lookupArea = lookupArea;
    }

    /**
     * Checks if lookup is simple(dimensions of lookup is only vertical or only
     * horizontal).
     * 
     * @return <code>True</code> if lookup is simple.
     */
    public boolean isSimpleGrid() {
        return isVerticalLookup() || isHorizontalLookup();
    }

    /**
     * @return <code>True</code> if lookup is simple vertical.
     */
    public boolean isVerticalLookup() {
        for (int i = 0; i < lookupArea.getWidth(); i++) {
            ValueEval value = lookupArea.getRelativeValue(0, i);
            if (!(value instanceof BlankEval)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return <code>True</code> if lookup is simple horizontal.
     */
    public boolean isHorizontalLookup() {
        for (int i = 0; i < lookupArea.getHeight(); i++) {
            ValueEval value = lookupArea.getRelativeValue(i, 0);
            if (!(value instanceof BlankEval)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates linearized {@link Grid} for lookup.
     * 
     * @return Lookup {@link Grid} in linearized form.
     */
    public Grid createLookupData() {
        Grid result;
        if (isHorizontalLookup()) {
            result = createTransposedGrid();
        } else {
            result = createGrid();
        }
        if (isSimpleGrid()) {
            return result;
        } else {
            return new GridDelegator(result);
        }
    }

    private Grid createGrid() {
        Grid result = new Grid();
        String values[][] = new String[lookupArea.getWidth()][lookupArea.getHeight()];
        for (int i = 0; i < lookupArea.getWidth(); i++) {
            for (int j = 0; j < lookupArea.getHeight(); j++) {
                ValueEval value = lookupArea.getRelativeValue(j, i);
                if (value instanceof StringValueEval) {
                    values[i][j] = ((StringValueEval) value).getStringValue();
                } else {
                    values[i][j] = "";
                }
            }
        }
        result.setGrid(values);
        return result;
    }

    private Grid createTransposedGrid() {
        Grid result = new Grid();
        String values[][] = new String[lookupArea.getHeight()][lookupArea.getWidth()];
        for (int i = 0; i < lookupArea.getWidth(); i++) {
            for (int j = 0; j < lookupArea.getHeight(); j++) {
                ValueEval value = lookupArea.getRelativeValue(j, i);
                if (value instanceof StringValueEval) {
                    values[j][i] = ((StringValueEval) value).getStringValue();
                } else {
                    values[j][i] = "";
                }
            }
        }
        result.setGrid(values);
        return result;
    }
}

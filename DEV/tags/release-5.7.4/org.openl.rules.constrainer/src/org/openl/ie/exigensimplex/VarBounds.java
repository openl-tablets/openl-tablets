package org.openl.ie.exigensimplex;

/**
 * <p>
 * Title: VarBounds
 * </p>
 * <p>
 * Description: The class is designed to keep an information about the
 * constraints imposed on a variable.
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

public class VarBounds {
    private int _type;
    private double _ub;
    private double _lb;

    /**
     * @param type One of five possible types of a constraint
     * @param lb The lower bound
     * @param ub The upper bound
     * @see VariableType
     */
    public VarBounds(int type, double lb, double ub) {
        _type = type;
        _ub = ub;
        _lb = lb;
    };

    /**
     * @return The lower bound
     */
    public double getLb() {
        return _lb;
    }

    /**
     * @return the type of a constraint
     * @see VariableType
     */
    public int getType() {
        return _type;
    }

    /**
     * @return The upper bound
     */
    public double getUb() {
        return _ub;
    }

    @Override
    public String toString() {
        String vartype = "";
        String sub = "", slb = "";
        switch (_type) {
            case VariableType.FREE_VARIABLE:
                vartype += "Free";
                break;
            case VariableType.BOUNDED_BELOW:
                vartype += "BoundedBelow";
                slb = " lb=" + _lb;
                break;
            case VariableType.BOUNDED_ABOVE:
                vartype += "BoundedAbove";
                sub = " ub=" + _ub;
                break;
            case VariableType.DOUBLE_BOUNDED:
                vartype += "DoubleBounded";
                slb = " lb=" + _lb;
                sub = " ub=" + _ub;
                break;
            case VariableType.FIXED_VARIABLE:
                vartype += "Fixed";
                slb = " lb=" + _lb;
                sub = " ub=" + _lb;
                break;

            default:
                break;
        }

        return "{" + "Type=" + vartype + sub + slb + "}";
    }

}
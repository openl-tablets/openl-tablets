package org.openl.ie.simplex;

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
public class VarType {
    /**
     * free variable: -inf < x[k] < +inf
     *
     * @see LPX#setColBnds(int, int, double, double)
     */
    static public final int LPX_FR = native_LPX_FR(); /**/
    /**
     * lower bound: l[k] <= x[k] < +inf
     *
     * @see LPX#setColBnds(int, int, double, double)
     * @see LPX#setRowBnds(int, int, double, double)
     */
    static public final int LPX_LO = native_LPX_LO(); /**/
    /**
     * upper bound: -inf < x[k] <= u[k]
     *
     * @see LPX#setColBnds(int, int, double, double)
     * @see LPX#setRowBnds(int, int, double, double)
     */
    static public final int LPX_UP = native_LPX_UP(); /**/
    /**
     * double bound: l[k] <= x[k] <= u[k]
     *
     * @see LPX#setColBnds(int, int, double, double)
     * @see LPX#setRowBnds(int, int, double, double)
     */
    static public final int LPX_DB = native_LPX_DB(); /**/
    /**
     * fixed variable: l[k] = x[k] = u[k]
     *
     * @see LPX#setColBnds(int, int, double, double)
     * @see LPX#setRowBnds(int, int, double, double)
     */
    static public final int LPX_FX = native_LPX_FX(); /**/

    private double _lb = -Double.MAX_VALUE;
    private double _ub = Double.MAX_VALUE;
    private int _type = LPX_FR;

    static private native int native_LPX_DB();

    static private native int native_LPX_FR();

    static private native int native_LPX_FX();

    static private native int native_LPX_LO();

    static private native int native_LPX_UP();

    public VarType(int type, double lb, double ub) {
        setType(type);
        if ((lb > ub) && ((type == LPX_FR) || (type == LPX_DB))) {
            throw new IllegalArgumentException("the upper bound must not be less then the lower bound");
        }
        _lb = lb;
        _ub = ub;
    }

    public double getLb() {
        if ((_type == LPX_FR) || (_type == LPX_UP)) {
            return (-Double.MAX_VALUE);
        }
        return _lb;
    }

    public int getType() {
        return _type;
    }

    public double getUb() {
        if ((_type == LPX_FR) || (_type == LPX_LO)) {
            return Double.MAX_VALUE;
        }
        return _ub;
    }

    public void setType(int type) {
        if (!((type == LPX_FR) || (type == LPX_LO) || (type == LPX_UP) || (type == LPX_DB) || (type == LPX_FX))) {
            throw new IllegalArgumentException("unsupported variable's bounds type");
        }
        _type = type;
    }

    @Override
    public String toString() {
        String vartype = "";
        String sub = "", slb = "";
        if (_type == LPX_FR) {
            vartype += "Free";
        }
        if (_type == LPX_LO) {
            vartype += "BoundedBelow";
            slb = " lb=" + _lb;
        }
        if (_type == LPX_UP) {
            vartype += "BoundedAbove";
            sub = " ub=" + _ub;
        }
        if (_type == LPX_DB) {
            vartype += "Bounded";
            slb = " lb=" + _lb;
            sub = " ub=" + _ub;
        }
        if (_type == LPX_FX) {
            vartype += "Fixed";
            slb = " lb=" + _lb;
            sub = " ub=" + _lb;
        }
        return "[" + "Type=" + vartype + sub + slb + "]";
    }

}
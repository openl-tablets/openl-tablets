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

public class VarInfo {
    // status of the variable returned by VarInfo.getStatus
    /**
     * basic variable
     *
     * @see VarInfo#getStatus()
     */
    static public final int LPX_BS = native_LPX_BS(); /**/
    /**
     * non-basic variable on lower bound
     *
     * @see VarInfo#getStatus()
     */
    static public final int LPX_NL = native_LPX_NL(); /**/
    /**
     * non-basic variable on upper bound
     *
     * @see VarInfo#getStatus
     */
    static public final int LPX_NU = native_LPX_NU(); /**/
    /**
     * non-basic free variable
     *
     * @see VarInfo#getStatus()
     */
    static public final int LPX_NF = native_LPX_NF(); /**/
    /**
     * non-basic fixed variable
     *
     * @see VarInfo#getStatus()
     */
    static public final int LPX_NS = native_LPX_NS(); /**/

    int _status;
    double _prim;
    double _dual;
    static {
        Status.dictionary.put(new Integer(LPX_BS), "basic");
        Status.dictionary.put(new Integer(LPX_NL), "non-basic_lb");
        Status.dictionary.put(new Integer(LPX_NU), "non-basic_ub");
        Status.dictionary.put(new Integer(LPX_NF), "non-basic_free");
        Status.dictionary.put(new Integer(LPX_NS), "non-basic_fixed");
    }

    private static native int native_LPX_BS();

    private static native int native_LPX_NF();

    private static native int native_LPX_NL();

    private static native int native_LPX_NS();

    private static native int native_LPX_NU();

    VarInfo(double[] parms) {
        setStatus((int) parms[0]);
        _prim = parms[1];
        _dual = parms[2];
    }

    public VarInfo(int status, double prim, double dual) {
        setStatus(status);
        _prim = prim;
        _dual = dual;
    }

    public double getDual() {
        return _dual;
    }

    public double getPrim() {
        return _prim;
    }

    public int getStatus() {
        return _status;
    }

    private boolean isLegalStatus(int status) {
        if (!((status == LPX_BS) || (status == LPX_NL) || (status == LPX_NU) || (status == LPX_NF) || (status == LPX_NS))) {
            return false;
        }
        return true;
    }

    private void setStatus(int status) {
        if (isLegalStatus(status)) {
            _status = status;
        } else {
            _status = LPX_NF;
        }
    }

    @Override
    public String toString() {
        String varinfo = "[Type=" + Status.translate(_status);
        if (_prim != 0) {
            varinfo += " prim=" + _prim;
        } else {
            varinfo += " dual=" + _dual;
        }
        varinfo += "]";
        return varinfo;

    }
}
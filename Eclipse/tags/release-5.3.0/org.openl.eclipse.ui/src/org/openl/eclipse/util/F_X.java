/*
 * Created on Sep 26, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author sam
 */
public class F_X {
    protected Map x_fx = new HashMap();

    static public Set addAll(Set from, Set to) {
        if (from == null || from.size() == 0) {
            return to;
        }

        if (to == null) {
            return new HashSet(from);
        }

        to.addAll(from);
        return to;
    }

    public boolean add(Object x, Object fx) {
        Set Fx = f(x);

        if (Fx == null) {
            Fx = new HashSet();
            x_fx.put(x, Fx);
        }

        return Fx.add(fx);
    }

    /**
     * Returns closure(x) relative to f().
     *
     * Definition: closure(x) relative to f() is the smallest set containing x
     * such that closure(x) contains f(closure(x)).
     *
     *
     */
    public Set closure(Collection x) {
        Set cx, fcx = new HashSet(x);

        do {
            cx = fcx;
            fcx = f(cx);

            // if (Debug.DEBUG)
            // {
            // Debug.debug("cx = " + cx);
            // Debug.debug("fcx = " + fcx);
            // }

            fcx = addAll(cx, fcx);

            // if (Debug.DEBUG)
            // {
            // Debug.debug("cx + fcx = " + fcx);
            // }
        } while (fcx.size() > cx.size());

        return cx;
    }

    public Set f(Object x) {
        return (Set) x_fx.get(x);
    }

    public Set f(Set X) {
        Set FX = null;

        for (Iterator it = X.iterator(); it.hasNext();) {
            FX = addAll(f(it.next()), FX);
        }

        return FX;
    }

    public Set F() {
        return f(X());
    }

    public boolean isF() {
        for (Iterator it = X().iterator(); it.hasNext();) {
            if (f(it.next()).size() > 1) {
                return false;
            }
        }
        return true;
    }

    public boolean remove(Object x, Object fx) {
        Set Fx = f(x);

        if (Fx != null && Fx.remove(fx)) {
            if (Fx.size() == 0) {
                x_fx.remove(x);
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer();

        for (Iterator itX = X().iterator(); itX.hasNext();) {
            Object x = itX.next();
            s.append(x).append("\n");
            for (Iterator itF = f(x).iterator(); itF.hasNext();) {
                Object fx = itF.next();
                s.append(" -> ").append(fx).append("\n");
            }
        }

        return s.toString();
    }

    public Set X() {
        return x_fx.keySet();
    }

}

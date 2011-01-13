/*
 * Created on Oct 1, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.util;

import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author sam
 */
public class XxY {
    protected F_X x_Y = new F_X();

    protected F_X y_X = new F_X();

    public void add(F_X that) {
        for (Iterator itX = that.X().iterator(); itX.hasNext();) {
            Object x = itX.next();
            for (Iterator itY = that.f(x).iterator(); itY.hasNext();) {
                Object y = itY.next();
                add(x, y);
            }
        }
    }

    public boolean add(Object x, Object y) {
        return x_Y.add(x, y) && y_X.add(y, x);
    }

    public void add(XxY that) {
        add(that.x_Y());
    }

    public boolean contains(Object x, Object y) {
        return X().contains(x) && Y().contains(y);
    }

    public void remove(F_X that) {
        for (Iterator itX = that.X().iterator(); itX.hasNext();) {
            Object x = itX.next();
            for (Iterator itY = that.f(x).iterator(); itY.hasNext();) {
                Object y = itY.next();
                remove(x, y);
            }
        }
    }

    public boolean remove(Object x, Object y) {
        return x_Y.remove(x, y) && y_X.remove(y, x);
    }

    public void remove(XxY that) {
        remove(that.x_Y());
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer();

        s.append("X -> Y\n").append(x_Y);
        s.append("Y -> X\n").append(y_X);

        return s.toString();
    }

    public Set X() {
        return x_Y.X();
    }

    public F_X x_Y() {
        return x_Y;
    }

    public Set Y() {
        return y_X.X();
    }

    public F_X y_X() {
        return y_X;
    }

}

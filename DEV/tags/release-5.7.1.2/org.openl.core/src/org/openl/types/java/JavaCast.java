/*
 * Created on May 21, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import org.openl.types.IOpenCast;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public abstract class JavaCast implements IOpenCast {

    Class<?> from;
    Class<?> to;
    int distance;
    boolean implicit;

    public JavaCast(Class<?> from, Class<?> to, int distance, boolean implicit) {
        this.from = from;
        this.to = to;
        this.distance = distance;
        this.implicit = implicit;
    }

    public IOpenClass getFrom() {
        return JavaOpenClass.getOpenClass(from);
    }

    public IOpenClass getTo() {
        return JavaOpenClass.getOpenClass(to);
    }

    public boolean isImplicit() {
        return false;
    }

}

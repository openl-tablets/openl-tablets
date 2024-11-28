package org.openl.rules.table.constraints;

/**
 * @author Andrei Astrouski
 */
public interface Constraint {

    String getValue();

    Object[] getParams();

    boolean check(Object... valuesToCheck);

}

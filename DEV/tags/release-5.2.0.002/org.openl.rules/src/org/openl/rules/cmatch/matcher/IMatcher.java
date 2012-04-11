package org.openl.rules.cmatch.matcher;

import org.openl.types.IOpenClass;

public interface IMatcher {
    public static final String OP_MATCH = "match";
    public static final String OP_MIN = "min";
    public static final String OP_MAX = "max";

    String getName();
    boolean match(Object var, Object checkValue);

    boolean isTypeSupported(IOpenClass type);
    Object fromString(String checkValue);
}

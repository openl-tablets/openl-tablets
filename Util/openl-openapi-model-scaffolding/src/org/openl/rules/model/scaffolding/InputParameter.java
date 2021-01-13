package org.openl.rules.model.scaffolding;

public interface InputParameter {

    String getName();

    TypeInfo getType();

    In getIn();

    enum In {

        PATH,
        QUERY,
        HEADER,
        COOKIE

    }
}

package org.openl.rules.model.scaffolding;

public interface InputParameter {

    String getFormattedName();

    String getOriginalName();

    TypeInfo getType();

    In getIn();

    enum In {

        PATH,
        QUERY,
        HEADER,
        COOKIE

    }
}

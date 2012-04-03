package org.openl.rules.dt.algorithm.evaluator;

public class DomainCanNotBeDefined extends Exception {

    private static final long serialVersionUID = -5183188461885882863L;

    public DomainCanNotBeDefined(String message, String paramName) {
        super(message);
        this.paramName = paramName;
    }

    String paramName;

    public String getParamName() {
        return paramName;
    }

    public String getMessage() {

        return "Domain cannot  be generated for [" + paramName + "]: " + super.getMessage();
    }

}

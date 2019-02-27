package org.openl.rules.dt.algorithm.evaluator;

public class DomainCanNotBeDefined extends Exception {

    private static final long serialVersionUID = -5183188461885882863L;
    private String paramName;

    DomainCanNotBeDefined(String message, String paramName) {
        super(message);
        this.paramName = paramName;
    }

    public String getMessage() {
        return "Domain cannot  be generated for [" + paramName + "]: " + super.getMessage();
    }

}

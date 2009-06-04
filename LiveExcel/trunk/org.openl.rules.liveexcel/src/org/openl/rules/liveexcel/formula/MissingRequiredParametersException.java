package org.openl.rules.liveexcel.formula;

public class MissingRequiredParametersException extends RuntimeException {

    private static final long serialVersionUID = 8682075113528151185L;

    public MissingRequiredParametersException(String msg) {
        super(msg);
    }

}

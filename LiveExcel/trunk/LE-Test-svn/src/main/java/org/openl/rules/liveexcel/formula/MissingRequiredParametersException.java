package org.openl.rules.liveexcel.formula;

import org.openl.rules.liveexcel.LiveExcelException;

public class MissingRequiredParametersException extends LiveExcelException {

    private static final long serialVersionUID = 8682075113528151185L;

    public MissingRequiredParametersException(String msg) {
        super(msg);
    }

}

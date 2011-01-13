package org.openl.ie.exigensimplex;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company:
 * </p>
 *
 * @author unascribed
 * @version 1.0
 */

public class NoSolutionException extends Exception {
    private int _errorCode;
    private String _errorMessage = "No additional inforamtion is available";

    public NoSolutionException(int errorCode) {
        _errorCode = errorCode;
    }

    // NoSolutionException(String errorMessage){ _errorMessage = errorMessage;}
    public NoSolutionException(int errorCode, String errorMessage) {
        _errorCode = errorCode;
        _errorMessage = errorMessage;
    }

    public int getErrorCode() {
        return _errorCode;
    }

    public String getErrorMessage() {
        return _errorMessage;
    }
}
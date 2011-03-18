/*
 * Created on Sep 5, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.launch;

import org.eclipse.debug.ui.console.IConsole;

/**
 *
 * @author sam
 */
public class OpenlStackTraceHyperlink extends AConsoleHyperlink {
    public OpenlStackTraceHyperlink(IConsole console, String url) {
        super(console, url);
    }

    public void linkActivated() {
        try {
            activateTextEditor(url);
        } catch (Exception e) {
            handleException(e);
        }
    }

    // /**
    // * Returns the stack trace element for this hyperlink.
    // */
    // protected StackTrace getStackTrace(String text) throws Exception
    // {
    // StackTrace stackTrace = StackTrace.fromString(text);
    //
    // if (stackTrace.line < 0) // bad line number
    // throw new Exception("getStackTrace: Bad line number");
    //
    // if (stackTrace.fileName == null)
    // throw new Exception("getStackTrace: Bad file name");
    //
    // return stackTrace;
    // }

}

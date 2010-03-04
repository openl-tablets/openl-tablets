/*
 * Created on Sep 5, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.rules.eclipse.xls.launching;

import org.eclipse.debug.ui.console.IConsole;
import org.openl.eclipse.launch.AConsoleHyperlink;
import org.openl.util.RuntimeExceptionWrapper;

/**
 *
 * @author sam
 */
public class XlsStackTraceHyperlink extends AConsoleHyperlink {
    public XlsStackTraceHyperlink(IConsole console, String url) {
        super(console, url);
    }

    /**
     * @see org.eclipse.debug.ui.console.IHyperlink#linkActivated()
     */
    public void linkActivated() {
        try {
            ExcelLauncher.launch(url);
        } catch (Exception e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

}

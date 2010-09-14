/*
 * Created on Sep 5, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.launch;

import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.console.IHyperlink;
import org.openl.main.SourceCodeURLConstants;

/**
 * @author sam
 *
 */
public class OpenlConsoleTracker implements IConsoleLineTracker, IHyperlinkFactory, SourceCodeURLConstants {
    
    private boolean isRegistered = false;
    private IConsole console;

    public IHyperlink createHyperlink(IConsole console, String url) {
        return new OpenlStackTraceHyperlink(console, url);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.debug.ui.console.IConsoleLineTracker#dispose()
     */
    public void dispose() {
    }

    public String getExcludeMatchString() {
        return "*.xls*";
    }

    public String getIncludeMatchString() {
        return AT_PREFIX + "*";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.debug.ui.console.IConsoleLineTracker#init(org.eclipse.debug.ui.console.IConsole)
     */
    public synchronized void init(IConsole console) {
        
        this.console = console;

        if (!isRegistered) {
            ConsoleLineTrackerTool.register(this, getIncludeMatchString(), getExcludeMatchString(),
                    ConsoleLineTrackerTool.DEFAULT_PRIORITY);
            isRegistered = true;
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.debug.ui.console.IConsoleLineTracker#lineAppended(org.eclipse.jface.text.IRegion)
     */
    public void lineAppended(IRegion line) {
        ConsoleLineTrackerTool.lineAppended(console, line);
    }

}

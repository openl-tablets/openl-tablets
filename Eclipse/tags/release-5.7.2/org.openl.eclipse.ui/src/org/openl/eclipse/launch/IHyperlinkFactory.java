/*
 * Created on Dec 24, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.launch;

import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.ui.console.IHyperlink;

/**
 *
 * @author sam
 */
public interface IHyperlinkFactory {
    public IHyperlink createHyperlink(IConsole console, String url);
}

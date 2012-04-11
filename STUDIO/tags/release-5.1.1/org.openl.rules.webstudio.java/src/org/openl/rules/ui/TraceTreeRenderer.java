/**
 * Created Jan 26, 2007
 */
package org.openl.rules.ui;

import org.openl.util.ITreeElement;

/**
 * @author snshor
 */
public class TraceTreeRenderer extends DTreeRenderer {

    static String[][] icons = {
            { "decision", "webresource/images/ruleset.gif", "webresource/images/ruleset-h.gif",
                    "webresource/images/ruleset-error.png", "webresource/images/ruleset-error.png" },
            { "rule", "webresource/images/test_ok.gif", "webresource/images/test_ok.gif",
                    "webresource/images/data-error.png", "webresource/images/data-error.png" } };

    /**
     * @param jsp
     * @param frame
     * @param icons
     */
    public TraceTreeRenderer(String jsp, String frame) {
        super(jsp, frame, icons);
    }

    protected String makeURL(ITreeElement element) {
        return targetJsp + "?elementID=" + map.getID(element);
    }

}

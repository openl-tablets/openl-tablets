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
                    "webresource/images/data-error.png", "webresource/images/data-error.png" },
            { "cmatch", "webresource/images/cmatch.gif", "webresource/images/cmatch.gif",
                    "webresource/images/cmatch-error.gif", "webresource/images/cmatch-error.gif" },
            { "cmResult", "webresource/images/cmatch-check.gif", "webresource/images/cmatch-check.gif",
                    "webresource/images/error.png", "webresource/images/webresource/images/error.png" },
            { "cmMatch", "webresource/images/cmatch-match.gif", "webresource/images/cmatch-match.gif",
                    "webresource/images/error.png", "webresource/images/error.png" },
            { "tbasic", "webresource/images/tbasic.gif", "webresource/images/tbasic.gif",
                    "webresource/images/tbasic-error.gif", "webresource/images/tbasic-error.gif" },
            { "tbasicMethod", "webresource/images/method.gif", "webresource/images/method.gif",
                    "webresource/images/error.png", "webresource/images/error.png" },
            { "tbasicOperation", "webresource/images/tbasic-operation.gif", "webresource/images/tbasic-operation.gif",
                    "webresource/images/error.png", "webresource/images/error.png" },
    };

    /**
     * @param jsp
     * @param frame
     * @param icons
     */
    public TraceTreeRenderer(String jsp, String frame) {
        super(jsp, frame, icons);
    }

    protected String makeURL(ITreeElement element) {
        return targetJsp + "?id=" + map.getID(element);
    }

    public void cacheElement(ITreeElement<?> element) {
        // TODO Auto-generated method stub
    }

}

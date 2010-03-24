/**
 * Created Jan 26, 2007
 */
package org.openl.rules.ui;

import java.util.Iterator;

import org.openl.base.INamedThing;
import org.openl.util.ITreeElement;
import org.openl.util.StringTool;

/**
 * @author snshor
 */
public abstract class DTreeRenderer {

    private static final int CLOSE = 0, OPEN = 1;

    static final int STATE_SHIFT = 2;

    static final String[] DEFAULT_ICON_NODE = { "default.node", "webresource/images/dtree/folder.png",
            "webresource/images/dtree/folder-open.png", "webresource/images/dtree/folder-error.png",
            "webresource/images/dtree/folder-open-error.png" }, DEFAULT_ICON_LEAF = { "default.leaf",
            "webresource/images/reg-text.png", "webresource/images/categoryset.gif" };

    static final public String GUESS_TYPE = "guess.type";

    String targetFrame = "mainFrame";

    String targetJsp;

    String[][] icons;

    ObjectMap map = new ObjectMap();

    static final int errIndex(ProjectTreeElement element) {
        return element.hasProblem() ? 1 : 0;
    }

    public static String jsStr(String string) {
        if (string == null) {
            return "''";
        }

        return "'" + string + "'";
    }

    public static String jsStrName(String string) {
        if (string == null) {
            return "''";
        }

        int idx = string.indexOf('\n');
        if (idx > 0) {
            string = string.substring(0, idx) + " ...";
        }

        return "'" + StringTool.encodeJavaScriptString(string) + "'";
    }

    public DTreeRenderer(String jsp, String frame, String[][] icons) {
        this.icons = icons;
        targetFrame = frame;
        targetJsp = jsp;
    }

    public abstract void cacheElement(ITreeElement<?> element);

    /**
     * @param obj
     * @return
     */
    protected String getCustomDisplayName(Object obj) {
        return String.valueOf(obj);
    }

    protected String getDisplayName(Object obj, int mode) {
        if (obj instanceof INamedThing) {
            INamedThing nt = (INamedThing) obj;
            return nt.getDisplayName(mode);
        }

        return getCustomDisplayName(obj);
    }

    private String getIcon(ITreeElement element, int open) {
        int state = getState(element);
        String type = getType(element);
        String[] iconset = guessIcon(element, type);
        if (iconset.length > 1 + open + STATE_SHIFT * state) {
            return iconset[1 + open + STATE_SHIFT * state];
        }
        return iconset[1 + open];
    }

    /**
     * @param element
     * @return
     */
    protected String getJavaType(ITreeElement element) {
        return StringTool.lastToken(element.getObject().getClass().getName(), ".").toLowerCase();
    }

    protected int getState(ITreeElement element) {
        return 0;
    }

    /**
     * @param element
     * @return
     */
    protected String getType(ITreeElement element) {
        String type = element.getType();
        if (type != null) {
            return type;
        }
        return GUESS_TYPE;
    }

    /**
     * @param element
     * @param type
     * @return
     */
    private String[] guessIcon(ITreeElement<?> element, String type) {
        if (type == GUESS_TYPE) {
            type = getJavaType(element);
        }

        int iconIndex = -1;
        int minStartPosition = 999999;
        int maxLengthOfMatchedWord = 0;

        // FIXME: Rewrite!!!!!

        for (int i = 0; i < icons.length; i++) {
            int idx = type.indexOf(icons[i][0]);
            if (0 <= idx && idx <= minStartPosition && maxLengthOfMatchedWord < icons[i][0].length()) {
                iconIndex = i;
                minStartPosition = idx;
                maxLengthOfMatchedWord = icons[i][0].length();
            }
        }

        String[] iconSet;

        if (iconIndex >= 0) {
            iconSet = icons[iconIndex];
        } else {
            iconSet = element.isLeaf() ? DEFAULT_ICON_LEAF : DEFAULT_ICON_NODE;
        }

        return iconSet;
    }

    protected abstract String makeURL(ITreeElement<?> element);

    public void renderElement(ITreeElement<?> parent, ITreeElement<?> element, StringBuffer buf) {
        renderSingleElement(parent, element, buf);
        for (Iterator iter = element.getChildren(); iter.hasNext();) {
            ITreeElement<?> child = (ITreeElement) iter.next();
            renderElement(element, child, buf);
        }
    }

    public String renderRoot(ITreeElement<?> element) {
        StringBuffer buf = new StringBuffer(1000);
        renderElement(null, element, buf);
        return buf.toString();
    }

    public void renderSingleElement(ITreeElement parent, ITreeElement element, StringBuffer buf) {
        cacheElement(element);

        buf.append("d.add(");

        int parentId = parent == null ? -1 : map.getID(parent);
        int id = map.getNewID(element);
        String url = makeURL(element);
        String name = getDisplayName(element, INamedThing.SHORT);
        String title = getDisplayName(element, INamedThing.REGULAR);
        String target = targetFrame;
        String icon = getIcon(element, CLOSE);
        String iconOpen = getIcon(element, OPEN);
        String open = null;

        buf.append(id).append(',');
        buf.append(parentId).append(',');
        buf.append(jsStrName(name)).append(',');
        buf.append(jsStr(url)).append(',');
        buf.append(jsStrName(title)).append(',');
        buf.append(jsStr(target)).append(',');
        buf.append(jsStr(icon)).append(',');
        buf.append(jsStr(iconOpen)).append(',');
        buf.append(jsStr(open));

        buf.append(");");
        buf.append("\n");
    }
}

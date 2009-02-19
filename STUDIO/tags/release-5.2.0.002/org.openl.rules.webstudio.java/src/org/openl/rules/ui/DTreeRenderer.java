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

    String targetFrame = "mainFrame";

    String targetJsp;

    String[][] icons;

    public DTreeRenderer(String jsp, String frame, String[][] icons) {
        this.icons = icons;
        this.targetFrame = frame;
        this.targetJsp = jsp;
    }

    protected String getDisplayName(Object obj, int mode) {
        if (obj instanceof INamedThing) {
            INamedThing nt = (INamedThing) obj;
            return nt.getDisplayName(mode);
        }

        return getCustomDisplayName(obj);
    }

    /**
     * @param obj
     * @return
     */
    protected String getCustomDisplayName(Object obj) {
        return String.valueOf(obj);
    }

    protected abstract String makeURL(ITreeElement<?> element);

    public String renderRoot(ITreeElement<?> element) {
        StringBuffer buf = new StringBuffer(1000);
        renderElement(null, element, buf);
        return buf.toString();
    }

    public void renderElement(ITreeElement<?> parent, ITreeElement<?> element, StringBuffer buf) {
        renderSingleElement(parent, element, buf);
        for (Iterator iter = element.getChildren(); iter.hasNext();) {
            ITreeElement<?> child = (ITreeElement) iter.next();
            renderElement(element, child, buf);
        }
    }

    public void renderSingleElement(ITreeElement parent, ITreeElement element, StringBuffer buf) {
        // d.add(id, parentId, name, url, title, target, icon, iconOpen, open) {

        buf.append("d.add(");
        int parentId = parent == null ? -1 : map.getID(parent);
        int id = map.getNewID(element);
        // String sfx = (element.getNameCount() < 2 ?
        // "":"("+element.getNameCount()+")");
        String name = getDisplayName(element, INamedThing.SHORT);
        String url = makeURL(element);
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

    public static String jsStr(String string) {
        if (string == null)
            return "''";

        return "'" + string + "'";
    }

    public static String jsStrName(String string) {
        if (string == null)
            return "''";

        int idx = string.indexOf('\n');
        if (idx > 0)
            string = string.substring(0, idx) + " ...";

        return "'" + StringTool.encodeJavaScriptString(string) + "'";
    }

    static final int errIndex(ProjectTreeElement element) {
        return element.hasProblem() ? 1 : 0;
    }

    static final int STATE_SHIFT = 2;

    static final String[] DEFAULT_ICON_NODE = { "default.node", "webresource/images/dtree/folder-c-n.gif",
            "webresource/images/dtree/folder-o-n.gif", "webresource/images/folder-c-error.png",
            "webresource/images/folder-o-error.png" }, DEFAULT_ICON_LEAF = { "default.leaf",
            "webresource/images/reg-text.png", "webresource/images/categoryset.gif" };

    private String getIcon(ITreeElement element, int open) {
        int state = getState(element);
        String type = getType(element);
        String[] iconset = guessIcon(element, type);
        if (iconset.length > 1 + open + STATE_SHIFT * state)
            return iconset[1 + open + STATE_SHIFT * state];
        return iconset[1 + open];
    }

    /**
     * @param element
     * @param type
     * @return
     */
    private String[] guessIcon(ITreeElement element, String type) {
        if (type == GUESS_TYPE)
            type = getJavaType(element);

        int minIndex = -1;
        int minDist = 999999;

        for (int i = 0; i < icons.length; i++) {
            int idx = type.indexOf(icons[i][0]);
            if (0 <= idx && idx < minDist) {
                minIndex = i;
                minDist = idx;
            }
        }

        if (minIndex >= 0)
            return icons[minIndex];

        return element.isLeaf() ? DEFAULT_ICON_LEAF : DEFAULT_ICON_NODE;
    }

    /**
     * @param element
     * @return
     */
    protected String getJavaType(ITreeElement element) {
        return StringTool.lastToken(element.getObject().getClass().getName(), ".").toLowerCase();
    }

    static final public String GUESS_TYPE = "guess.type";

    /**
     * @param element
     * @return
     */
    protected String getType(ITreeElement element) {
        String type = element.getType();
        if (type != null)
            return type;
        return GUESS_TYPE;
    }

    protected int getState(ITreeElement element) {
        return 0;
    }

    ObjectMap map = new ObjectMap();

}

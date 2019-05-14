package org.openl.rules.webstudio.web.trace;

/**
 * A FancyTree tree node item.
 *
 * @author Yury Molchan
 */
public class TraceNode {
    private boolean lazy;
    private String title;
    private String tooltip;
    private int key = -1;
    private String extraClasses;

    public boolean isLazy() {
        return lazy;
    }

    public void setLazy(boolean lazy) {
        this.lazy = lazy;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTooltip() {
        return tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public String getExtraClasses() {
        return extraClasses;
    }

    public void setExtraClasses(String extraClasses) {
        this.extraClasses = extraClasses;
    }
}

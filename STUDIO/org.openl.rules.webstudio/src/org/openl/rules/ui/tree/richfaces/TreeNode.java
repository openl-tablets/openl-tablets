package org.openl.rules.ui.tree.richfaces;

import org.richfaces.model.TreeNodeImpl;

public class TreeNode extends TreeNodeImpl {

    private String name;
    private String title;
    private String url;
    @Deprecated
    private int state;
    @Deprecated
    private int numErrors;
    private String type;
    private boolean active;

    public TreeNode() {
        this(false);
    }

    public TreeNode(boolean isLeaf) {
        super(isLeaf);
    }

    public TreeNode(String name, String title, String url, int state, int numErrors, String type, boolean active) {
        this(false, name, title, url, state, numErrors, type, active);
    }

    public TreeNode(boolean isLeaf,
            String name,
            String title,
            String url,
            int state,
            int numErrors,
            String type,
            boolean active) {
        this(isLeaf);
        this.name = name;
        this.title = title;
        this.url = url;
        this.state = state;
        this.numErrors = numErrors;
        this.type = type;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getNumErrors() {
        return numErrors;
    }

    public void setNumErrors(int numErrors) {
        this.numErrors = numErrors;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

}

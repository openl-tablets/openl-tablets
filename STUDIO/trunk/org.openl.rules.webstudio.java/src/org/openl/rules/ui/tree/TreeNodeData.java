package org.openl.rules.ui.tree;

public class TreeNodeData {

    private String name;
    private String title;
    private String url;
    private int state;
    private String type;
    private boolean active;

    public TreeNodeData() {
    }

    public TreeNodeData(String name, String title, String url, int state,
            String type, boolean active) {
        this.name = name;
        this.title = title;
        this.url = url;
        this.state = state;
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

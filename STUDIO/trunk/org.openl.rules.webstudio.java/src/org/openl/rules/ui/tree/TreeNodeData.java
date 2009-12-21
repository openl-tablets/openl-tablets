package org.openl.rules.ui.tree;

public class TreeNodeData {

    private String name;
    private String title;
    private String url;
    private String icon;
    private String type;

    public TreeNodeData() {
    }

    public TreeNodeData(String name, String title, String url, String icon, String type) {
        this.name = name;
        this.title = title;
        this.url = url;
        this.icon = icon;
        this.type = type;
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}

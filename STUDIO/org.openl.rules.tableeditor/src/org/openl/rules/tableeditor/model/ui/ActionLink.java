package org.openl.rules.tableeditor.model.ui;

public class ActionLink {

    private String name;
    private String action;

    public ActionLink() {
    }

    public ActionLink(String name, String action) {
        super();
        this.name = name;
        this.action = action;
    }

    /**
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param action the action to set
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

}

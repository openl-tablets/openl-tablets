package org.openl.rules.webstudio.web.repository;

import java.io.Serializable;

/**
 * 
 * @author Aleh Bykhavets
 *
 */
public abstract class AbstractItem implements Serializable, Comparable<AbstractItem> {
    private static final long serialVersionUID = -6017864869385004280L;

    /** Boolean flag. If this entry is currently 'selected'. */
    private boolean selected;

    /** Item name. */
    private String name;

    /** Messages associated with this entry. */
    private String messages;

    /** Style of the name */
    private String styleForName;

    /** Style of the message */
    private String styleForMessages;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessages() {
        return messages;
    }

    public void setMessages(String messages) {
        this.messages = messages;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getStyleForName() {
        return styleForName;
    }

    public void setStyleForName(String styleForName) {
        this.styleForName = styleForName;
    }

    public String getStyleForMessages() {
        return styleForMessages;
    }

    public void setStyleForMessages(String styleForMessages) {
        this.styleForMessages = styleForMessages;
    }

    /**
     * By default items are ordered by name.
     */
    public int compareTo(AbstractItem o) {
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        
        if (obj instanceof AbstractItem) {
            AbstractItem o = (AbstractItem) obj;

            return name.equals(o.name);
        } else return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

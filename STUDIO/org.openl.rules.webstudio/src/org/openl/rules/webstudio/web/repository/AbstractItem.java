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

    /**
     * By default items are ordered by name.
     */
    @Override
    public int compareTo(AbstractItem o) {
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof AbstractItem) {
            AbstractItem o = (AbstractItem) obj;

            return name.equals(o.name);
        } else {
            return false;
        }
    }

    public String getMessages() {
        return messages;
    }

    public String getName() {
        return name;
    }

    public String getStyleForMessages() {
        return styleForMessages;
    }

    public String getStyleForName() {
        return styleForName;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setMessages(String messages) {
        this.messages = messages;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setStyleForMessages(String styleForMessages) {
        this.styleForMessages = styleForMessages;
    }

    public void setStyleForName(String styleForName) {
        this.styleForName = styleForName;
    }
}

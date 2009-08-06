package org.openl.rules.tableeditor.renderkit;

/**
 * Temporary class for holding table properties
 * @author DLiauchuk
 *
 */
public class TableProperty {
    
    private String displayName;
    private Object value;
    private Class<?> type;
    private boolean show;
    private boolean canEdit;
    private String group;
    private String name;
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TableProperty(String displayName, Object value, Class<?> type, String group) {
        this.displayName = displayName;
        this.value = value;
        this.type = type;
        this.group = group;
    }
    
    public TableProperty(String displayName, Object value, Class<?> type, String group, String name) {
        this.displayName = displayName;
        this.value = value;
        this.type = type;
        this.group = group;
        this.name = name;
    }
    
    public boolean isShow() {
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }
    
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Object getValue() {
        Object result;
        if (type == null || (value == null && isStringValue())) {
            result = new String("");
        } else {
            result = value;            
        }
        return result;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean isStringValue() {        
        boolean stringValue = false;
        if (type.equals(String.class)) {
            stringValue = true;
        }        
        return stringValue;
    }

    public boolean isDateValue() {
        boolean dateValue = false;
        if (type.equals(java.util.Date.class)) {
            dateValue = true;
        }
        return dateValue;
    }

    public boolean isBooleanValue() {
        boolean booleanValue = false;
        if(type.equals(java.lang.Boolean.class)) {
            booleanValue = true;
        }        
        return booleanValue;
    }

}

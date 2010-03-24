package org.openl.rules.tableeditor.renderkit;

import java.text.SimpleDateFormat;
import java.util.Date;

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
    private String format;
    
    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

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
    
    public TableProperty(String displayName, Object value, Class<?> type, 
            String group, String name, String format) {
        this.displayName = displayName;
        this.value = value;
        this.type = type;
        this.group = group;
        this.name = name;
        this.format = format;
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
        if (type == null || (value == null && isStringType())) {
            result = new String("");
        } else {
            result = value;            
        }
        return result;
    }

    /**
     * This method must be used for all the cases when you need to display property on UI.
     * It converts its value from any type to string.
     * @return
     */
    public String getValueString() {
        String result = "";
        if(value != null) {
            if (value instanceof String) {
                result = (String)value;
            } else {
                if (value instanceof Date) {
                    SimpleDateFormat sdf = new SimpleDateFormat(getFormat());
                    result = sdf.format((Date)value);
                } else {
                    if (value instanceof Boolean) {
                        result = ((Boolean)value).toString();                    
                    } else {
                        if (value instanceof Integer) {
                            // if date format for cell was not set in excel, it will process as
                            // general and we will get the integer value on UI.
                            // We must display it as Date.  
//                            if (type != null && isDateType()) {
//                                Date dateFromInt = DateUtil.getJavaDate(((Integer)value).doubleValue());
//                                SimpleDateFormat sdf = new SimpleDateFormat(getFormat());
//                                result = sdf.format(dateFromInt);
//                            } else {
                                result = ((Integer)value).toString();
//                            }
                            
                        } else {
                            if (value instanceof Double) {
                                result = ((Double)value).toString();
                            }
                        }
                    }            
                }
            }
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

    public boolean isStringType() {        
        boolean stringValue = false;
        if (type.equals(String.class)) {
            stringValue = true;
        }        
        return stringValue;
    }

    public boolean isDateType() {
        boolean dateValue = false;
        if (type.equals(java.util.Date.class)) {
            dateValue = true;
        }
        return dateValue;
    }

    public boolean isBooleanType() {
        boolean booleanValue = false;
        if(type.equals(java.lang.Boolean.class)) {
            booleanValue = true;
        }        
        return booleanValue;
    }
    
    public boolean isDoubleType() {
        boolean doubleValue = false;
        if(type.equals(java.lang.Double.class)) {
            doubleValue = true;
        }        
        return doubleValue;
    }
    
    

}

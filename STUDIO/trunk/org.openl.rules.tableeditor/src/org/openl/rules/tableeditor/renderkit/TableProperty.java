package org.openl.rules.tableeditor.renderkit;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.properties.def.TablePropertyDefinition.InheritanceLevel;
import org.openl.rules.table.properties.inherit.InheritanceLevelChecker;
import org.openl.rules.table.properties.inherit.InvalidPropertyLevelException;

/**
 * Temporary class for holding table properties
 * @author DLiauchuk
 *
 */
public class TableProperty {
    private String name;
    private String displayName;
    private Object value;
    private Class<?> type;
    
    private String group;    
    private String format;
    private Constraints constraints;
    private String description;
    private boolean system;
    
    private TableProperty(TablePropertyBuilder builder) {
        this.name = builder.name;
        this.displayName = builder.displayName;
        this.value = builder.value;
        this.type = builder.type;
        this.group = builder.group;
        this.format = builder.format;
        this.constraints = builder.constraints;
        this.description = builder.description;
        this.system = builder.system;
    }
    
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
    
    /**
     * 
     * @return <code>TRUE</code> if property value can be overriden on TABLE level.
     */
    public boolean canBeOverridenInTable() {
        boolean result = false;
        try {
            InheritanceLevelChecker.checkPropertyLevel(InheritanceLevel.TABLE, name);
            result = true;
        } catch (InvalidPropertyLevelException e) {            
        }
        return result;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Object getValue() {        
        return value;
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

    public boolean isString() {        
        return String.class.equals(type);
    }

    public boolean isDate() {
        return Date.class.equals(type);
    }

    public boolean isBoolean() {
        return Boolean.class.equals(type);
    }
    
    public boolean isDouble() {
        return Double.class.equals(type);
    }

    public void setConstraints(Constraints constraints) {
        this.constraints = constraints;
    }

    public Constraints getConstraints() {
        return constraints;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setSystem(boolean system) {
        this.system = system;        
    }
    
    public boolean isSystem() {
        return system;        
    }    
    
    /**
     * Builder for TableProperties
     * @author DLiauchuk
     *
     */
    public static class TablePropertyBuilder {
        // Required parameters
        private String name;
        private String displayName;                
        
        // Optional parameters
        private Object value;
        private Class<?> type;
        private String group;
        private String format;
        private Constraints constraints;
        private String description;
        private boolean system;
        
        public TablePropertyBuilder(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
        }
        
        public TablePropertyBuilder value(Object val) {
            value = val;
            return this;
        }
        
        public TablePropertyBuilder type(Class<?> val) {
            type = val;
            return this;
        }
        
        public TablePropertyBuilder group(String val) {
            group = val;
            return this;
        }
        
        public TablePropertyBuilder format(String val) {
            format = val;
            return this;
        }
        
        public TablePropertyBuilder constraints(Constraints val) {
            constraints = val;
            return this;
        }
        
        public TablePropertyBuilder description(String val) {
            description = val;
            return this;
        }
        
        public TablePropertyBuilder system(boolean val) {
            system = val;
            return this;
        }
        
        public TableProperty build() {
            return new TableProperty(this);
        }
    }
}

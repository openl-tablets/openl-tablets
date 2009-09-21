package org.openl.meta;

import java.util.Date;

public class ObjectValue implements IMetaHolder, Comparable<ObjectValue> {
    
    private IMetaInfo metaInfo;
    private Object value;
    
    public ObjectValue(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Error initializing ObjectValue class. Parameter \"value\" can't be null.");
        }
        this.value = value;
        metaInfo = new ValueMetaInfo();
    }
    
    public ObjectValue(Object value, String shortName, String fullName, String sourceUrl) {
        if (value == null) {
            throw new IllegalArgumentException("Error initializing ObjectValue class. Parameter \"value\" can't be null.");
        }
        this.value = value;
        metaInfo = new ValueMetaInfo(shortName, fullName, sourceUrl);
    }
    
    public IMetaInfo getMetaInfo() {        
        return metaInfo;
    }

    public void setMetaInfo(IMetaInfo info) {
        this.metaInfo = info;        
    }    
    
    public Object getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object obj) {

        if (obj instanceof ObjectValue) {
            ObjectValue v = (ObjectValue) obj;
            return value.equals(v.value);
        }
        if (obj instanceof Object) {
            Object s = (Object) obj;
            return value.equals(s);
        }

        return false;
    }
    
    @Override
    public int hashCode() {
        return value.hashCode();
    }    
    
    public int compareTo(ObjectValue objectToCompare) {
        int result = -1;
        if(value instanceof String && objectToCompare.getValue() instanceof String) {
            result = ((String)value).compareTo(((String)objectToCompare.getValue()));
        } else {
            if(value instanceof Date && objectToCompare.getValue() instanceof Date) {
                result = ((Date)value).compareTo(((Date)objectToCompare.getValue()));
            } else {
                if(value instanceof Boolean && objectToCompare.getValue() instanceof Boolean) {
                    result = ((Boolean)value).compareTo(((Boolean)objectToCompare.getValue()));
                } else {
                    if(value instanceof Integer && objectToCompare.getValue() instanceof Integer) {
                        result = ((Integer)value).compareTo(((Integer)objectToCompare.getValue()));
                    }
                }
            }
        }   
        return result;
    }

}

package org.openl.rules.lang.xls.utils;

public class StringToBooleanCaster implements ICustomCaster<String, Boolean> {

    public Boolean cast(String valueToCast) {        
        String value = valueToCast;
        Boolean result = new Boolean(value);
        return result;        
    }

}

package org.openl.rules.lang.xls.utils;

import java.util.Date;

import org.apache.poi.ss.usermodel.DateUtil;

public class DoubleToDateCaster implements ICustomCaster<Double, Date> {

    public Date cast(Double valueToCast) {
        Double value = valueToCast;
        Date result = null;        
        result = DateUtil.getJavaDate(value);
        return result;
    }
    
}

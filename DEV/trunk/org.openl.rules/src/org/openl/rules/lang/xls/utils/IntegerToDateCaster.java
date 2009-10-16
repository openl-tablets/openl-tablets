package org.openl.rules.lang.xls.utils;

import java.util.Date;

import org.apache.poi.ss.usermodel.DateUtil;

public class IntegerToDateCaster implements ICustomCaster<Integer, Date> {

    public Date cast(Integer valueToCast) {
        Integer value = valueToCast;
        Date result = null;
        double doubleDate = value.doubleValue();
        result = DateUtil.getJavaDate(doubleDate);
        return result;        
    }
}

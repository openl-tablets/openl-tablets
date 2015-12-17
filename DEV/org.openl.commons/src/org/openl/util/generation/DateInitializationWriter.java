package org.openl.util.generation;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateInitializationWriter implements TypeInitializationWriter {

    private static final String DATE_FORMAT_FOR_DATE_CONSTRUCTOR = "MM/dd/yyyy hh:mm:ss a";

    public String getInitialization(Object value) {
        Date date = (Date) value;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT_FOR_DATE_CONSTRUCTOR);
        String stringValue = simpleDateFormat.format(date);
        return String.format("new %s(\"%s\")", JavaClassGeneratorHelper.filterTypeName(value.getClass()), stringValue);
    }
}

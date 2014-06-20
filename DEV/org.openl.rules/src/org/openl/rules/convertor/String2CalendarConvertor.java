package org.openl.rules.convertor;

import java.util.Calendar;
import java.util.Date;

class String2CalendarConvertor implements IString2DataConvertor<Calendar> {

    @Override
    public String format(Calendar data, String format) {
        if (data == null) return null;
        return new String2DateConvertor().format(data.getTime(), format);
    }

    @Override
    public Calendar parse(String data, String format) {
        if (data == null) return null;

        Date date = new String2DateConvertor().parse(data, format);
        Calendar calendar = Calendar.getInstance(LocaleDependConvertor.getLocale());
        calendar.setTime(date);

        return calendar;
    }
}

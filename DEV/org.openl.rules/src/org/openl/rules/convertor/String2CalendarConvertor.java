package org.openl.rules.convertor;

import java.util.Calendar;

class String2CalendarConvertor implements IString2DataConvertor<Calendar> {
    @Override
    public Calendar parse(String data, String format) {
        if (data == null) {
            return null;
        }

        var date = new String2DateConvertor().parse(data, format);
        Calendar calendar = Calendar.getInstance(LocaleDependConvertor.getLocale());
        calendar.setTime(date);

        return calendar;
    }
}

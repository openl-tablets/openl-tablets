package org.openl.rules.convertor;

import java.util.Calendar;
import java.util.Date;

import org.openl.binding.IBindingContext;

public class String2CalendarConvertor extends LocaleDependConvertor implements IString2DataConvertor {

    public String format(Object data, String format) {
        return new String2DateConvertor().format(((Calendar) data).getTime(), format);
    }

    public Object parse(String data, String format, IBindingContext cxt) {
        return parseCalendar(data, format);
    }

    public Calendar parseCalendar(String data, String format) {

        Date d = new String2DateConvertor().parseDate(data, format);
        Calendar c = Calendar.getInstance(getLocale());
        c.setTime(d);

        return c;
    }

}

package org.openl.rules.convertor;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

class String2DateConvertor implements IString2DataConvertor<Date> {

    @Override
    public Date parse(String data, String format) {
        if (data == null) {
            return null;
        }
        if (format != null) {
            DateFormat df = new SimpleDateFormat(format, LocaleDependConvertor.getLocale());
            try {
                return df.parse(data);
            } catch (ParseException e) {
                throw new IllegalArgumentException(
                    "Cannot convert \"" + data + "\" to date type using: \"" + format + "\" format");
            }
        }

        List<DateFormat> dateFormats = Arrays.asList(
            DateFormat.getDateInstance(DateFormat.SHORT, LocaleDependConvertor.getLocale()),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", LocaleDependConvertor.getLocale()),
            new SimpleDateFormat("yyyy-MM-dd", LocaleDependConvertor.getLocale()));
        for (DateFormat dateFormat : dateFormats) {
            try {
                dateFormat.setLenient(false);
                dateFormat.getCalendar().set(0, 0, 0, 0, 0, 0);
                dateFormat.getCalendar().set(Calendar.MILLISECOND, 0);
                Date date = dateFormat.parse(data);
                return date;
            } catch (ParseException e) {
                // Loop on
            }
        }
        throw new IllegalArgumentException("Cannot convert \"" + data + "\" to Date type");
    }

}

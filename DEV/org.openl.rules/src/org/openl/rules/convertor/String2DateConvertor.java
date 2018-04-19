package org.openl.rules.convertor;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class String2DateConvertor implements IString2DataConvertor<Date> {

    private static final int YEAR_START_COUNT = 1900;
    private final Logger log = LoggerFactory.getLogger(String2DateConvertor.class);

    @Override
    public Date parse(String data, String format) {
        if (data == null) {
            return null;
        }
        if (data.isEmpty()) {
            throw new IllegalArgumentException("Cannot convert an empty String to date type");
        }

        DateFormat df;
        if (format == null) {
            df = DateFormat.getDateInstance(DateFormat.SHORT, LocaleDependConvertor.getLocale());
        } else {
            df = new SimpleDateFormat(format, LocaleDependConvertor.getLocale());
        }
        df.setLenient(false);
        df.getCalendar().set(0, 0, 0, 0, 0, 0);
        df.getCalendar().set(Calendar.MILLISECOND, 0);

        try {
            return df.parse(data);
        } catch (ParseException e) {
            try {
                int value = Integer.parseInt(data);
                Calendar cc = Calendar.getInstance();
                cc.set(YEAR_START_COUNT, 0, 1);
                cc.add(Calendar.DATE, value - 1);
                return cc.getTime();

            } catch (NumberFormatException t) {
                log.debug(t.getMessage(), t);
            }
            throw new IllegalArgumentException("Cannot convert \"" + data + "\" to date type");
        }
    }
}

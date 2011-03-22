package org.openl.rules.convertor;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.binding.IBindingContext;
import org.openl.util.RuntimeExceptionWrapper;

public class String2DateConvertor extends LocaleDependConvertor implements IString2DataConvertor {

    private static final Log LOG = LogFactory.getLog(String2DateConvertor.class);
    private static final int YEAR_START_COUNT = 1900;

    private DateFormat defaultFormat = DateFormat.getDateInstance(DateFormat.SHORT, getLocale());

    public String format(Object data, String format) {
        DateFormat df = format == null ? DateFormat.getDateInstance(DateFormat.SHORT) : new SimpleDateFormat(format);
        return df.format(data);
    }

    public Object parse(String data, String format, IBindingContext cxt) {
        return parseDate(data, format);
    }

    public Date parseDate(String data, String format) {
        DateFormat df = format == null ? defaultFormat : new SimpleDateFormat(format, getLocale());

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
                LOG.debug(t);
            }
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }
}

package org.openl.domain;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

/**
 * Domain for range of dates.
 *
 * @author PUdalau
 */
public class DateRangeDomain implements IDomain<Date> {
    private class DateIterator implements Iterator<Date> {
        private Calendar current;

        DateIterator() {
            current = (Calendar) min.clone();
            current.add(Calendar.DAY_OF_MONTH, -1);
        }

        @Override
        public boolean hasNext() {
            return current.before(max);
        }

        @Override
        public Date next() {
            current.add(Calendar.DAY_OF_MONTH, 1);
            return current.getTime();
        }

        @Override
        public void remove() {
        }
    }

    private Calendar min = new GregorianCalendar();
    private Calendar max = new GregorianCalendar();

    /**
     * Creates date range inside the specified bounds(including bounds).
     *
     * @param min left bound.
     * @param max right bound.
     */
    public DateRangeDomain(Date min, Date max) {
        setMin(min);
        setMax(max);
    }

    /**
     * @return The left bound of range.
     */
    public Date getMin() {
        return min.getTime();
    }

    /**
     * @return The right bound of range.
     */
    public Date getMax() {
        return max.getTime();
    }

    /**
     * Sets left bound of range.
     */
    public void setMin(Date min) {
        this.min.setTime(truncate(min));
    }

    /**
     * Sets right bound of range.
     */
    public void setMax(Date max) {
        this.max.setTime(truncate(max));
    }

    private Date truncate(Date min) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(min);
        Date date = cal.getTime();
        long time = date.getTime();

        // see http://issues.apache.org/jira/browse/LANG-59
        time -= cal.get(Calendar.MILLISECOND);
        time -= cal.get(Calendar.SECOND) * 1000;
        time -= cal.get(Calendar.MINUTE) * 1000 * 60;
        // reset time
        if (date.getTime() != time) {
            date.setTime(time);
            cal.setTime(date);
        }

        cal.set(Calendar.HOUR_OF_DAY, 0);
        return cal.getTime();
    }

    @Override
    public Iterator<Date> iterator() {
        return new DateIterator();
    }

    private static final long ONE_HOUR = 60 * 60 * 1000L;

    public static long daysBetween(Calendar d1, Calendar d2) {
        return (d2.getTimeInMillis() - d1.getTimeInMillis() + ONE_HOUR) / (ONE_HOUR * 24);
    }

    public int size() {
        return (int) daysBetween(min, max) + 1;
    }

    @Override
    public IType getElementType() {
        return null;
    }

    @Override
    public boolean selectObject(Date obj) {
        return obj.before(max.getTime()) && obj.after(min.getTime());
    }

    /**
     * @param value Date to get index.
     * @return The index of specified date or negative number if specified date does not belong to the range.
     */
    public int getIndex(Date value) {
        if (value.after(getMax())) {
            return -1;
        }

        Calendar date = new GregorianCalendar();
        date.setTime(value);
        return (int) daysBetween(min, date);
    }

    /**
     * @param index Index of the date.
     * @return Returns The date within the range or <code>null</code> if date with specified index does not belong to
     *         the range.
     */
    public Date getValue(int index) {
        if (index >= size()) {
            return null;
        }
        Calendar date = (Calendar) min.clone();
        date.add(Calendar.DATE, index);
        return date.getTime();
    }

    @Override
    public String toString() {
        return "[" + getMin() + ";" + getMax() + "]";
    }
}

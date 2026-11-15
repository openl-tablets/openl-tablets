/**
 * Created Feb 12, 2007
 */
package org.openl.rules.dt.type.domains;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

import lombok.Getter;
import lombok.Setter;

import org.openl.domain.EnumDomain;

/**
 * The values of a list, such as the values of an alias datatype, numbered from 0.
 *
 * <p>Numbers are numbered in ascending order, so the numbers within a range have consecutive indexes. Other values
 * keep the order of the list, and so do numbers among which is NaN or an infinity.
 *
 * @author snshor
 */
public class EnumDomainAdaptor implements IDomainAdaptor {

    @Getter
    @Setter
    private Object[] values;

    /** Whether every value is a finite number, so the values are in ascending order. */
    @Getter
    private final boolean numeric;

    public EnumDomainAdaptor(EnumDomain<?> domain) {
        values = domain.getAllObjects();
        numeric = values.length > 0 && Arrays.stream(values).allMatch(EnumDomainAdaptor::isFiniteNumber);
        if (numeric) {
            values = values.clone();
            Arrays.sort(values, Comparator.comparing(EnumDomainAdaptor::decimalOf));
        }
    }

    /** A number other than NaN and the infinities, which have no decimal value to order them by. */
    private static boolean isFiniteNumber(Object value) {
        return value instanceof Number number
                && !(number instanceof Double d && !Double.isFinite(d))
                && !(number instanceof Float f && !Float.isFinite(f));
    }

    /** The value of a number, whatever its type. */
    public static BigDecimal decimalOf(Object number) {
        return new BigDecimal(number.toString());
    }

    @Override
    public int getIndex(Object value) {

        for (var i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public int getMax() {
        return values.length - 1;
    }

    @Override
    public int getMin() {
        return 0;
    }

    @Override
    public Object getValue(int index) {
        return values[index];
    }

    public int size() {
        return values.length;
    }

    @Override
    public IDomainAdaptor merge(IDomainAdaptor adaptor) {
        var a = (EnumDomainAdaptor) adaptor;

        var v1 = getValues();
        var v2 = a.getValues();

        var set = new HashSet<Object>(v1.length + v2.length);

        set.addAll(Arrays.asList(v1));

        set.addAll(Arrays.asList(v2));

        var newValues = set.toArray();

        return new EnumDomainAdaptor(new EnumDomain<>(newValues));

    }

}

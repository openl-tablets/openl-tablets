/**
 * Created Feb 28, 2007
 */
package org.openl.rules.table.xls.formatters;

/**
 * @author snshor
 *
 */
public interface IFormatter {
    String format(Object obj);
    Object parse(String value);
}

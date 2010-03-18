/**
 * Created Feb 28, 2007
 */
package org.openl.rules.table.ui;

/**
 * @author snshor
 *
 */
public interface ITextFormatter {
    String format(Object obj);
    Object parse(String value);
}

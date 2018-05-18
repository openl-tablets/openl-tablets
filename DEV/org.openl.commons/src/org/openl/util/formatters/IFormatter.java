package org.openl.util.formatters;

/**
 * @author snshor
 * 
 */
public interface IFormatter {
    String format(Object obj);

    Object parse(String value);
}

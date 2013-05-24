package org.openl.util.formatters;

import org.openl.base.INamedThing;
import org.openl.util.print.Formatter;

/**
 * 
 * Wrapper to adapt {@link Formatter} functionality to {@link IFormatter}.
 * Supports only format operation.
 * 
 * @author DLiauchuk
 * 
 */
public class FormatterAdapter implements IFormatter {

    public String format(Object obj) {
        StringBuilder buf = new StringBuilder();
        return Formatter.format(obj, INamedThing.REGULAR, buf).toString();
    }

    public Object parse(String value) {
        throw new UnsupportedOperationException("Should not be called, this is only to implement IFormatter interface.");
    }

}

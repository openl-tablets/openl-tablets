/*
 * Created on Nov 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import org.openl.binding.IBindingContext;

/**
 * @author snshor
 *
 */
public interface IString2DataConvertor {
    public String format(Object data, String format);

    // boolean isBindingContextRequired
    // public Object convertArray(String[] data);

    public Object parse(String data, String format, IBindingContext cxt);

    // public Object makeArray(int size);

}

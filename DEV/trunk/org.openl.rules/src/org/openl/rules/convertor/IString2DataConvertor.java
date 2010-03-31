/*
 * Created on Nov 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;

/**
 * @author snshor
 *
 */
public interface IString2DataConvertor {
    
    String format(Object data, String format);

    Object parse(String data, String format, IBindingContext bindingContext);
}

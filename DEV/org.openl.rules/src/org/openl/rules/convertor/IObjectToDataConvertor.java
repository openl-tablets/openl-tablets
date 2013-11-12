package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;

/**
 * Common interface of convertor object from one type to another.
 * 
 * @author PUdalau
 */
public interface IObjectToDataConvertor {
    Object convert(Object data, IBindingContext bindingContext);
}

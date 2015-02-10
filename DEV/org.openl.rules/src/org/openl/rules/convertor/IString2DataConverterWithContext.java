package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;

public interface IString2DataConverterWithContext<T> {

    T parse(String data, String format, IBindingContext cxt);

}

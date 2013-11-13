package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;

public class String2CharConvertor implements IString2DataConvertor {

    public String format(Object data, String forma) {
        return new String(new char[] { ((Character) data).charValue() });
    }

    public Object parse(String data, String format, IBindingContext cxt) {
        if (data.length() != 1) {
            throw new IndexOutOfBoundsException("Character field must have only one symbol");
        }

        return new Character(data.charAt(0));
    }

}

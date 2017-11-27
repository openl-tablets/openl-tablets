package org.openl.rules.convertor;

class String2ShortConvertor extends String2IntegersConvertor<Short> {

    String2ShortConvertor() {
        super(Short.MIN_VALUE, Short.MAX_VALUE);
    }

    @Override
    Short toNumber(long number) {
        return (short) number;
    }
}

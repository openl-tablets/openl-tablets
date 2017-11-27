package org.openl.rules.convertor;

class String2IntConvertor extends String2IntegersConvertor<Integer> {

    String2IntConvertor() {
        super(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Override
    Integer toNumber(long number) {
        return (int) number;
    }
}

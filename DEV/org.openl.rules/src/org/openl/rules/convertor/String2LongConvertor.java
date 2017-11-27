package org.openl.rules.convertor;

class String2LongConvertor extends String2IntegersConvertor<Long> {

    String2LongConvertor() {
        super(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    @Override
    Long toNumber(long number) {
        return number;
    }
}

package org.openl.rules.convertor;

class String2ByteConvertor extends String2IntegersConvertor<Byte> {

    String2ByteConvertor() {
        super(Byte.MIN_VALUE, Byte.MAX_VALUE);
    }

    @Override
    Byte toNumber(long number) {
        return (byte) number;
    }
}

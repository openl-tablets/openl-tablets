package org.openl.rules.convertor;

class String2CharConvertor implements IString2DataConvertor<Character> {

    @Override
    public Character parse(String data, String format) {
        if (data == null) {
            return null;
        }
        if (data.length() != 1) {
            throw new IndexOutOfBoundsException("Character field must have only one symbol");
        }
        return data.charAt(0);
    }
}

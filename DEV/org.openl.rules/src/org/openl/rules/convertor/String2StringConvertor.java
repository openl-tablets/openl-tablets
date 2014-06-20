package org.openl.rules.convertor;

class String2StringConvertor implements IString2DataConvertor<String> {

    @Override
    public String format(String data, String format) {
        return data;
    }

    @Override
    public String parse(String data, String format) {
        return data;
    }
}

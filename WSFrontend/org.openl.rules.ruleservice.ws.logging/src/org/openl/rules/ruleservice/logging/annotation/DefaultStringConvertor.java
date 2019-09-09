package org.openl.rules.ruleservice.logging.annotation;

import org.openl.rules.ruleservice.logging.Convertor;

public final class DefaultStringConvertor implements Convertor<Object, String> {
    @Override
    public String convert(Object value) {
        throw new UnsupportedOperationException();
    }
}

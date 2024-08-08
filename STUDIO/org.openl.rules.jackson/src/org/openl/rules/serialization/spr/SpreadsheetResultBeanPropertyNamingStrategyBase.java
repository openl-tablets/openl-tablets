package org.openl.rules.serialization.spr;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;

import org.openl.rules.calc.SpreadsheetCell;
import org.openl.util.StringUtils;

abstract class SpreadsheetResultBeanPropertyNamingStrategyBase extends PropertyNamingStrategy {

    protected abstract String transform(String name);

    protected abstract String transform(String column, String row);

    protected String toUpperCamelCase(String input) {
        if (input == null || input.length() == 0) {
            return input;
        }
        char c = input.charAt(0);
        char uc = Character.toUpperCase(c);
        if (c == uc) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input);
        sb.setCharAt(0, uc);
        return sb.toString();
    }

    protected String toLowerCamelCase(String input) {
        if (input == null || input.length() == 0) {
            return input;
        }
        char c = input.charAt(0);
        char uc = Character.toLowerCase(c);
        if (c == uc) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input);
        sb.setCharAt(0, uc);
        return sb.toString();
    }

    @Override
    public String nameForGetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
        return getName(method, defaultName);
    }

    @Override
    public String nameForSetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
        return getName(method, defaultName);
    }

    private String getName(AnnotatedMember member, String defaultName) {
        if (member.hasAnnotation(SpreadsheetCell.class)) {
            SpreadsheetCell spreadsheetCell = member.getAnnotation(SpreadsheetCell.class);
            if (StringUtils.isEmpty(spreadsheetCell.column())) {
                return transform(spreadsheetCell.row());
            } else if (StringUtils.isEmpty(spreadsheetCell.row())) {
                return transform(spreadsheetCell.column());
            } else {
                return transform(spreadsheetCell.column(), spreadsheetCell.row());
            }
        }
        return defaultName;
    }
}

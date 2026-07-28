package org.openl.rules.serialization.spr;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;

import org.openl.rules.calc.SpreadsheetCell;
import org.openl.rules.calc.SpreadsheetResultBeanPropertyNamingStrategy;
import org.openl.util.JavaKeywordUtils;
import org.openl.util.StringUtils;

abstract class SpreadsheetResultBeanPropertyNamingStrategyBase extends PropertyNamingStrategy implements SpreadsheetResultBeanPropertyNamingStrategy {

    @Override
    public abstract String transform(String name);

    @Override
    public abstract String transform(String column, String row);

    protected String toUpperCamelCase(String input) {
        input = JavaKeywordUtils.toJavaIdentifier(input);
        var c = input.charAt(0);
        var uc = Character.toUpperCase(c);
        if (c == uc) {
            return input;
        }
        var sb = new StringBuilder(input);
        sb.setCharAt(0, uc);
        return sb.toString();
    }

    protected String toLowerCamelCase(String input) {
        input = JavaKeywordUtils.toJavaIdentifier(input);
        var c = input.charAt(0);
        var uc = Character.toLowerCase(c);
        if (c == uc) {
            return input;
        }
        var sb = new StringBuilder(input);
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
            var spreadsheetCell = member.getAnnotation(SpreadsheetCell.class);
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

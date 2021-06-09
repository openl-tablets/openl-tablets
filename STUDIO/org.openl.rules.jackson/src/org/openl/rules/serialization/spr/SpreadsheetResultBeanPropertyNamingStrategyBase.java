package org.openl.rules.serialization.spr;

import java.lang.reflect.Field;
import java.util.Objects;

import org.openl.rules.calc.SpreadsheetCell;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;

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

    protected String transform(SpreadsheetCell spreadsheetCell) {
        if (spreadsheetCell.simpleRefByColumn()) {
            return transform(spreadsheetCell.column());
        } else if (spreadsheetCell.simpleRefByRow()) {
            return transform(spreadsheetCell.row());
        } else {
            return transform(spreadsheetCell.column(), spreadsheetCell.row());
        }
    }

    @Override
    public String nameForField(MapperConfig<?> config, AnnotatedField field, String defaultName) {
        if (field.hasAnnotation(SpreadsheetCell.class)) {
            SpreadsheetCell spreadsheetCell = field.getAnnotation(SpreadsheetCell.class);
            String ret = transform(spreadsheetCell);
            int c = 0;
            int duplicates = 0;
            for (Field f : field.getDeclaringClass().getDeclaredFields()) {
                SpreadsheetCell sc = f.getAnnotation(SpreadsheetCell.class);
                if (sc != null) {
                    if (Objects.equals(ret, transform(sc))) {
                        duplicates++;
                    }
                }
            }
            return duplicates < 2 ? ret : defaultName;
        }
        boolean g = false;
        for (Field f : field.getDeclaringClass().getDeclaredFields()) {
            if (f.isAnnotationPresent(SpreadsheetCell.class)) {
                g = true;
                break;
            }
        }
        if (g) {
            return transform(defaultName);
        }
        return defaultName;
    }
}

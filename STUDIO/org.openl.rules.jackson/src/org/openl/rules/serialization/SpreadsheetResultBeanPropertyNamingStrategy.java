package org.openl.rules.serialization;

/*-
 * #%L
 * OpenL - STUDIO - Jackson
 * %%
 * Copyright (C) 2016 - 2020 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import org.openl.rules.calc.SpreadsheetResultBean;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;

public class SpreadsheetResultBeanPropertyNamingStrategy {

    protected static abstract class SpreadsheetResultBeanPropertyNamingStrategyBase extends PropertyNamingStrategy {

        protected abstract PropertyNamingStrategy withPropertyNamingStrategy();

        @Override
        public String nameForField(MapperConfig<?> config, AnnotatedField field, String defaultName) {
            if (field.getDeclaringClass().isAnnotationPresent(SpreadsheetResultBean.class)) {
                return withPropertyNamingStrategy().nameForField(config, field, defaultName);
            }
            return defaultName;
        }

        @Override
        public String nameForGetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
            if (method.getDeclaringClass().isAnnotationPresent(SpreadsheetResultBean.class)) {
                return withPropertyNamingStrategy().nameForGetterMethod(config, method, defaultName);
            }
            return defaultName;
        }

        @Override
        public String nameForSetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
            if (method.getDeclaringClass().isAnnotationPresent(SpreadsheetResultBean.class)) {
                return withPropertyNamingStrategy().nameForSetterMethod(config, method, defaultName);
            }
            return defaultName;
        }

        @Override
        public String nameForConstructorParameter(MapperConfig<?> config,
                AnnotatedParameter ctorParam,
                String defaultName) {
            if (ctorParam.getDeclaringClass().isAnnotationPresent(SpreadsheetResultBean.class)) {
                return withPropertyNamingStrategy().nameForConstructorParameter(config, ctorParam, defaultName);
            }
            return defaultName;
        }
    }

    public static class UpperCamelCaseStrategy extends SpreadsheetResultBeanPropertyNamingStrategyBase {
        public UpperCamelCaseStrategy() {
            this(false);
        }

        public UpperCamelCaseStrategy(boolean removeUnderscores) {
            this.removeUnderscores = removeUnderscores;
        }

        @Override
        protected PropertyNamingStrategy withPropertyNamingStrategy() {
            return new PropertyNamingStrategy.PropertyNamingStrategyBase() {
                @Override
                public String translate(String input) {
                    if (input == null || input.length() == 0) {
                        return input; // garbage in, garbage out
                    }
                    // Replace first lower-case letter with upper-case equivalent
                    char c = input.charAt(0);
                    char uc = Character.toUpperCase(c);
                    StringBuilder sb = new StringBuilder(input);
                    sb.setCharAt(0, uc);
                    int i = 0;
                    while (i < sb.length()) {
                        char ch = sb.charAt(i);
                        if (ch == '_' && i + 1 < sb.length()) {
                            i++;
                            sb.setCharAt(i, Character.toUpperCase(sb.charAt(i)));
                        }
                        i++;
                    }
                    return removeUnderscores ? sb.toString().replaceAll("_", "") : sb.toString();
                }
            };
        }

        private final boolean removeUnderscores;
    }

    public static class NoUnderscoresUpperCamelCaseStrategy extends SpreadsheetResultBeanPropertyNamingStrategyBase {
        @Override
        protected PropertyNamingStrategy withPropertyNamingStrategy() {
            return new SpreadsheetResultBeanPropertyNamingStrategy.UpperCamelCaseStrategy(true);
        }
    }

    public static class LowerCaseStrategy extends SpreadsheetResultBeanPropertyNamingStrategyBase {
        @Override
        protected PropertyNamingStrategy withPropertyNamingStrategy() {
            return new PropertyNamingStrategy.LowerCaseStrategy();
        }
    }

    public static class LowerCamelStrategy extends SpreadsheetResultBeanPropertyNamingStrategyBase {
        public LowerCamelStrategy() {
            this(false);
        }

        public LowerCamelStrategy(boolean removeUnderscores) {
            this.removeUnderscores = removeUnderscores;
        }

        protected PropertyNamingStrategy withPropertyNamingStrategy() {
            return new PropertyNamingStrategy.PropertyNamingStrategyBase() {
                @Override
                public String translate(String input) {
                    if (input == null || input.length() == 0) {
                        return input; // garbage in, garbage out
                    }
                    // Replace first lower-case letter with upper-case equivalent
                    char c = input.charAt(0);
                    char uc = Character.toLowerCase(c);
                    StringBuilder sb = new StringBuilder(input);
                    sb.setCharAt(0, uc);
                    int i = 0;
                    while (i < sb.length()) {
                        char ch = sb.charAt(i);
                        if (ch == '_' && i + 1 < sb.length()) {
                            i++;
                            sb.setCharAt(i, Character.toLowerCase(sb.charAt(i)));
                        }
                        i++;
                    }
                    return removeUnderscores ? sb.toString().replaceAll("_", "") : sb.toString();
                }
            };
        }

        private final boolean removeUnderscores;
    }

    public static class NoUnderscoresLowerCamelStrategy extends SpreadsheetResultBeanPropertyNamingStrategyBase {
        @Override
        protected PropertyNamingStrategy withPropertyNamingStrategy() {
            return new SpreadsheetResultBeanPropertyNamingStrategy.LowerCamelStrategy(true);
        }
    }

}

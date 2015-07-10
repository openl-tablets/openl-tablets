package org.openl.rules.calculation.result.convertor2;

/*
 * #%L
 * OpenL - DEV - Rules - Calculation Result
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import org.apache.commons.lang3.ClassUtils;
import org.openl.rules.convertor.IObjectToDataConvertor;
import org.openl.rules.convertor.ObjectToDataConvertorFactory;
import org.openl.util.StringTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

public class SpreadsheetColumnExtractor<S extends CalculationStep> {

    private final Logger log = LoggerFactory.getLogger(SpreadsheetColumnExtractor.class);

    public SpreadsheetColumnExtractor(ColumnToExtract column) {
        this.column = column;
    }

    /**
     * column to extract
     */
    private ColumnToExtract column;

    public ColumnToExtract getColumn() {
        return column;
    }

    public void setColumn(ColumnToExtract column) {
        this.column = column;
    }

    /**
     * Convert the given value to the appropriate type that is expected. And
     * store it to the row instance.
     *
     * @param valueForStoraging
     * @param spreadsheetRow for population with given data
     */
    public void convertAndStoreData(Object valueForStoraging, S spreadsheetRow) {
        for (String propertyName : column.getPropertyNames()) {
            if (valueForStoraging != null) {
                Object value = convert(valueForStoraging, column.getExpectedType(propertyName));
                if (column.getExpectedType(propertyName).isAssignableFrom(value.getClass())){
                    if (store(value, spreadsheetRow, propertyName, column.getExpectedType(propertyName))) {
                        return;
                    }
                }
            }
        }
    }

    private boolean store(Object value, S step, String propertyName, Class<?> expectedType) {
        Method setterMethod = getSetterMethod(step, propertyName, expectedType);
        if (setterMethod != null) {
            try {
                setterMethod.invoke(step, value);
                return true;
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
        } else {
            if (log.isWarnEnabled()) {
                log.warn("Cannot find setter in {} class for [{}] column",
                    step.getClass().getName(),
                    column.getColumnName());
            }
        }
        return false;
    }

    private Method getSetterMethod(S step, String propertyName, Class<?> expectedType) {
        Method setterMethod = null;
        // try to get setter, by upper case the first symbol in the column name,
        // and leave the other part as is
        //
        String setterName = StringTool.getSetterName(propertyName);
        try {
            setterMethod = step.getClass().getMethod(setterName, expectedType);
        } catch (Exception e) {
            try {
                // try to get setter by upper case the first symbol in the
                // column name, and lower
                // case the rest
                //
                setterName = getSetterName(propertyName);
                setterMethod = step.getClass().getMethod(setterName, expectedType);
            } catch (Exception e1) {
                log.warn(e1.getMessage(), e1);
            }
        }
        return setterMethod;
    }

    // protected for tests
    protected String getSetterName(String fieldName) {
        return String.format("set%s%s", fieldName.substring(0, 1).toUpperCase(), fieldName.substring(1).toLowerCase());
    }

    private Object convert(Object x, Class<?> expectedType) {
        if (x.getClass().isArray() && expectedType.isArray()){
            int length = Array.getLength(x);
            Object newValue = Array.newInstance(expectedType.getComponentType(), length);
            IObjectToDataConvertor convertor = ObjectToDataConvertorFactory.getConvertor(expectedType.getComponentType(),
                x.getClass().getComponentType());
            for (int i = 0; i < length; i++) {
                Object componentValue = Array.get(x, i);
                if (!ClassUtils.isAssignable(componentValue.getClass(), expectedType.getComponentType())) {
                    try {
                        componentValue = convertor.convert(componentValue, null);
                    } catch (Exception e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Cannot convert value {} to {}", componentValue, expectedType.getComponentType()
                                .getName(), e);
                        }
                        return x;
                    }
                }
                Array.set(newValue, i, componentValue);
            }
            return newValue;
        }else{
            if (!ClassUtils.isAssignable(x.getClass(), expectedType)) {
                try {
                    IObjectToDataConvertor convertor = ObjectToDataConvertorFactory.getConvertor(expectedType, x.getClass());
                    return convertor.convert(x, null);
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Cannot convert value {} to {}", x, expectedType.getName(), e);
                    }
                }
            }
        }
        return x;
    }
}

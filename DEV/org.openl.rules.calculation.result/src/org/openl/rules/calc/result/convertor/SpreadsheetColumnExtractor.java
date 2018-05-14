package org.openl.rules.calc.result.convertor;

/*
 * #%L
 * OpenL - DEV - Rules - Calculation Result
 * %%
 * Copyright (C) 2015 - 2016 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.lang.reflect.Method;

import org.openl.rules.convertor.IObjectToDataConvertor;
import org.openl.rules.convertor.ObjectToDataConvertorFactory;
import org.openl.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class SpreadsheetColumnExtractor<S extends CalculationStep> {

    private final Logger log = LoggerFactory.getLogger(SpreadsheetColumnExtractor.class);

    public SpreadsheetColumnExtractor(ColumnToExtract column, boolean mandatory) {
        this.column = column;
        this.mandatory = mandatory;
    }

    /**
     * column to extract
     */
    private ColumnToExtract column;

    private boolean mandatory;

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

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
     * @param spreadsheetRow    for population with given data
     */
    public void convertAndStoreData(Object valueForStoraging, S spreadsheetRow) {
        if (valueForStoraging != null) {
            Object value = convert(valueForStoraging);
            store(value, spreadsheetRow);
        }
    }

    private void store(Object value, S step) {
        Method setterMethod = getSetterMethod(step);
        if (setterMethod != null) {
            try {
                setterMethod.invoke(step, value);
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
        } else {
            log.warn("Cannot find setter in {} class for [{}] column", step.getClass().getName(), column.getColumnName());
        }
    }

    private Method getSetterMethod(S step) {
        Method setterMethod = null;
        // try to get setter, by upper case the first symbol in the column name,
        // and leave the other part as is
        //
        String setterName = ClassUtils.setter(column.getColumnName());
        try {
            setterMethod = step.getClass().getMethod(setterName, column.getExpectedType());
        } catch (Exception e) {
            try {
                // try to get setter by upper case the first symbol in the
                // column name, and lower
                // case the rest
                //
                setterName = getSetterName(column.getColumnName());
                setterMethod = step.getClass().getMethod(setterName, column.getExpectedType());
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

    private Object convert(Object x) {
        if (needConversion(x)) {
            IObjectToDataConvertor convertor = ObjectToDataConvertorFactory.getConvertor(column.getExpectedType(),
                    x.getClass());
            try {
                return convertor.convert(x);
            } catch (Exception e) {
                log.warn("Cannot convert value {} to {}", x, column.getExpectedType().getName(), e);
            }
        }
        return x;
    }

    private boolean needConversion(Object x) {
        return !ClassUtils.isAssignable(x.getClass(), column.getExpectedType());
    }
}

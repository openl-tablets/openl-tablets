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

import java.lang.reflect.Method;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpreadsheetColumnExtractor<S extends CalculationStep> {

    private final Logger log = LoggerFactory.getLogger(SpreadsheetColumnExtractor.class);

    private NestedSpreadsheetConfiguration<? extends CalculationStep, ? extends CompoundStep> conf;

    /**
     * column to extract
     */
    private ColumnToExtract column;

    public SpreadsheetColumnExtractor(ColumnToExtract column,
            NestedSpreadsheetConfiguration<? extends CalculationStep, ? extends CompoundStep> configuration) {
        this.column = column;
        this.conf = configuration;
    }

    /**
     * Gets the configuration
     * 
     * @return {@link NestedSpreadsheetConfiguration}
     */
    public NestedSpreadsheetConfiguration<? extends CalculationStep, ? extends CompoundStep> getConfiguration() {
        return conf;
    }

    public ColumnToExtract getColumn() {
        return column;
    }

    public void setColumn(ColumnToExtract column) {
        this.column = column;
    }

    /**
     * Convert the given value to the appropriate type that is expected. And store it to the row instance.
     *
     * @param valueForStoraging
     * @param step for population with given data
     */
    public Object convertAndStoreData(Object valueForStoraging, S step) {
        if (valueForStoraging != null) {
            String[] propertyNames = column.getPropertyNames();
            Class<?>[] expectedTypes = column.getExpectedTypes();

            IOpenCast theBestCast = null;
            Class<?> type = Object.class;
            String propertyName = null;
            int minConvertDistance = Integer.MAX_VALUE;

            // Find the best cast:
            for (int i = 0; i < expectedTypes.length; i++) {
                Class<?> expectedType = expectedTypes[i];
                IOpenCast openCast = getConfiguration().getObjectToDataOpenCastConvertor()
                    .getConvertor(expectedType, valueForStoraging.getClass());
                if (openCast != null && openCast.getDistance() < minConvertDistance) {
                    theBestCast = openCast;
                    minConvertDistance = openCast.getDistance();
                    type = expectedType;
                    propertyName = propertyNames[i];
                }
            }

            if (theBestCast != null) {
                Object value = theBestCast.convert(valueForStoraging);
                if (store(value, step, propertyName, type)) {
                    return propertyName;
                }
            }
        }
        return null;
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

    // FIXME: performance sensitive code. It search setter every time for the given step.
    private Method getSetterMethod(S step, String propertyName, Class<?> expectedType) {
        Method setterMethod = null;
        // try to get setter, by upper case the first symbol in the column name,
        // and leave the other part as is
        //
        String setterName = ClassUtils.setter(propertyName);
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

}

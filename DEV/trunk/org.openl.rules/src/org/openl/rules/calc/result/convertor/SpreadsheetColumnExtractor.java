package org.openl.rules.calc.result.convertor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.convertor.IObjectToDataConvertor;
import org.openl.rules.convertor.ObjectToDataConvertorFactory;
import org.openl.util.StringTool;

public class SpreadsheetColumnExtractor<S extends CalculationStep> {
    
    private static final Log LOG = LogFactory.getLog(SpreadsheetColumnExtractor.class);
    
    public SpreadsheetColumnExtractor(ColumnToExtract column, boolean mandatory) {        
        this.column = column;
        this.mandatory = mandatory;
    }
    
    /** column to extract*/
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
     * Convert the given value to the appropriate type that is expected. And store it to the row instance.
     * 
     * @param valueForStoraging
     * @param spreadsheetRow for population with given data
     */
    public void convertAndStoreData(Object valueForStoraging, S spreadsheetRow) {
        if (valueForStoraging != null) { 
            Object value = convert(valueForStoraging);
            store(value, spreadsheetRow);
        }
    }

    private void store(Object value, S step) {
        Method setterName = null;
        try {
            setterName = step.getClass().getMethod(StringTool.getSetterName(column.getColumnName()), column.getExpectedType());
        } catch (SecurityException e) {
            LOG.warn(e);
        } catch (NoSuchMethodException e) {
            LOG.warn(e);
            try {
                // for fields that use only Upper case letters (e.g ID).
                setterName = step.getClass().getMethod(getSetterName(column.getColumnName()), column.getExpectedType());
            } catch (SecurityException e1) {               
                LOG.warn(e);
            } catch (NoSuchMethodException e1) {
                LOG.warn(e);
            }
        }        
        try {            
            setterName.invoke(step, value);
        } catch (IllegalArgumentException e) {
            LOG.warn(e);
        } catch (IllegalAccessException e) {
            LOG.warn(e);
        } catch (InvocationTargetException e) {
            LOG.warn(e);
        }
    }
    
    //protected for tests 
    protected String getSetterName(String fieldName) {
        return String.format("set%s%s", fieldName.substring(0,1).toUpperCase(), fieldName.substring(1).toLowerCase());
    }

    private Object convert(Object x) {        
        if (needConversion(x)) {
            IObjectToDataConvertor convertor = ObjectToDataConvertorFactory.getConvertor(column.getExpectedType(), 
                x.getClass());        
            return convertor.convert(x, null);
        } 
        return x;
    }
        
    private boolean needConversion(Object x) {
        return !ClassUtils.isAssignable(x.getClass(), column.getExpectedType());        
    }
}

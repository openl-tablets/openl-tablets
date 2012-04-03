package org.openl.rules.calc.result.convertor;

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
        Method setterMethod = getSetterMethod(step);        
        if (setterMethod != null) {
        	try {  
                setterMethod.invoke(step, value);
            } catch (Exception e) {
                 LOG.warn(e);
            } 
        } else {
        	String message = String.format("Cannot find setter in %s class for [%s] column", 
    				step.getClass().getName(), column.getColumnName());
        	LOG.warn(message);
        }
    }
    
    private Method getSetterMethod(S step) {
    	Method setterMethod = null;
    	// try to get setter, by upper case the first symbol in the column name, and leave the other part as is
    	//
    	String setterName = StringTool.getSetterName(column.getColumnName());
        try {
            setterMethod = step.getClass().getMethod(setterName, column.getExpectedType());
        } catch (Exception e) {            
            try {
                // try to get setter by upper case the first symbol in the column name, and lower
            	// case the rest
            	//
            	setterName = getSetterName(column.getColumnName());
                setterMethod = step.getClass().getMethod(setterName, column.getExpectedType());
            } catch (Exception e1) {               
            	LOG.warn(e1);
            }
        } 
        return setterMethod;
    }
    
    //protected for tests 
    protected String getSetterName(String fieldName) {
        return String.format("set%s%s", fieldName.substring(0,1).toUpperCase(), fieldName.substring(1).toLowerCase());
    }

    private Object convert(Object x) {        
        if (needConversion(x)) {
            IObjectToDataConvertor convertor = ObjectToDataConvertorFactory.getConvertor(column.getExpectedType(), 
                x.getClass());     
            try {
            	return convertor.convert(x, null);
            } catch (Exception e) {
            	String message = String.format("Cannot convert value %s to %s", x, column.getExpectedType().getName());
            	LOG.warn(message, e);
			}
        } 
        return x;
    }
        
    private boolean needConversion(Object x) {
        return !ClassUtils.isAssignable(x.getClass(), column.getExpectedType());        
    }
}

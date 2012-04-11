package org.openl.rules.convertor;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openl.binding.IBindingContext;
import org.openl.util.Log;
import org.openl.util.StringTool;

/**
 * Temporary class. 
 * 
 *
 */
public class String2ArrayConvertor implements IString2DataConvertor {

    /**
     * Constant for escaping {@link #ARRAY_ELEMENTS_SEPARATOR} of elements. It is needed when the element contains 
     * separator as part of object name, e.g: Mike\\,Sara`s Son.     
     * 
     */
    public static final String ARRAY_ELEMENTS_SEPARATOR_ESCAPER = "\\";
    
    /**
     * Separator for elements of array, represented as <code>{@link String}</code>.
     */
    public static final String ARRAY_ELEMENTS_SEPARATOR = ",";

    private IString2DataConvertor elementFormat;
    
    /**
     * @param elementFormat formatter for the component type of array.
     */
    public String2ArrayConvertor(IString2DataConvertor elementFormat) {
        this.elementFormat = elementFormat;
    }
    
    /**
     * Converts an input array of elements to <code>{@link String}</code>. Elements in the return value will separated by 
     * {@link #ARRAY_ELEMENTS_SEPARATOR}. Null safety.
     * 
     * @param value array of elements that should be represented as <code>{@link String}</code>. 
     * @return <code>{@link String}</code> representation of the income array. <code>NULL</code> if the income value is
     * <code>NULL</code> or if income value is not an array.
     */    
    public String format(Object value, String format) {
        String result = null;
        if (value != null) {
            if (!(value.getClass().isArray())) {
                Log.error(String.format("Should be an array: ", value.toString()));
                return result;
            }
    
            Object[] array = (Object[]) value;
                    
            String[] elementResults = new String[array.length];
            
            for (int i=0; i<array.length; i++) {
                Object element = array[i];
                elementResults[i] = elementFormat.format(element, format);
                result = StringUtils.join(elementResults, ",");
            }
        }
        return result;
    }
    
    /** 
     * 
     * @param value <code>{@link String}</code> representation of the array.
     * @return array of elements. <code>NULL</code> if input is empty or can`t get the component type of the array.  
     */
    public Object parse(String data, String format, IBindingContext bindingContext) {        
        Object result = null;
        if (StringUtils.isNotEmpty(data)) {
            String[] elementValues = StringTool.splitAndEscape(data, ARRAY_ELEMENTS_SEPARATOR,
                    ARRAY_ELEMENTS_SEPARATOR_ESCAPER);
            
            List<Object> elements = new ArrayList<Object>();
            Class<?> elementType = null;         
            
            for (String elementValue : elementValues) {
                Object element = elementFormat.parse(elementValue, format, bindingContext);
                elements.add(element);
                elementType = element.getClass();            
            }        
            
            if (elementType == null) {
                return result;
            }
            
            Object[] resultArray = (Object[])Array.newInstance(elementType, elements.size());        
            result = elements.toArray(resultArray);
        }
        return result;
    }

}

package org.openl.rules.table.xls;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.table.FormattedCell;
import org.openl.util.Log;
import org.openl.util.StringTool;

public class XlsArrayFormat extends XlsFormat {

    public static final String ARRAY_ELEMENTS_SEPARATOR_ESCAPER = "\\";
    public static final String ARRAY_ELEMENTS_SEPARATOR = ",";

    private XlsFormat elementFormat;

    public XlsArrayFormat(XlsFormat elementFormat) {
        this.elementFormat = elementFormat;
    }

    @Override
    public String format(Object value) {

        if (!(value.getClass().isArray())) {

            Log.error(String.format("Should be an array: ", value.toString()));
            return null;
        }

        Object[] array = (Object[]) value;
        String result = StringUtils.EMPTY;

        for (Object element : array) {

            String elementResult = elementFormat.format(element);
            result = StringUtils.join(new Object[] { result, elementResult }, ",");
        }

        return result;
    }

    @Override
    public Object parse(String value) {

        String[] elementValues = StringTool.splitAndEscape(value, ARRAY_ELEMENTS_SEPARATOR,
                ARRAY_ELEMENTS_SEPARATOR_ESCAPER);
        
        List<Object> elements = new ArrayList<Object>();
        Class<?> elementType = null;         
        
        for (String elementValue : elementValues) {

            Object element = elementFormat.parse(elementValue);
            elements.add(element);
            elementType = element.getClass();
            
        }        
        
        if (elementType == null) {
            return null;
        }
        
        Object[] resultArray = (Object[])Array.newInstance(elementType,elements.size());
        
        return elements.toArray(resultArray); 
    }

    public FormattedCell filterFormat(FormattedCell cell) {
        return cell;
    }

}

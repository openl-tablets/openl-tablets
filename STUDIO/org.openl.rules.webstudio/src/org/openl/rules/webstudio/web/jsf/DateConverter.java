package org.openl.rules.webstudio.web.jsf;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.el.ValueBinding;


/**
 * Special date converter to suppress rich:calendar conversion errors.
 *
 * @author Andrey Naumenko
 */
public class DateConverter implements Converter {
    // Ad hoc date value. Indicates conversion error.
    private static final Date SPECIAL_DATE = new Date(0);
    public static final java.lang.String CONVERTER_ID = "org.openl.date";

    public Object getAsObject(FacesContext facesContext, UIComponent uiComponent,
        String value) throws ConverterException
    {
        ValueBinding vb = uiComponent.getValueBinding("datePattern");
        String datePattern = (String) vb.getValue(facesContext);
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        try {
            return (new SimpleDateFormat(datePattern)).parse(value);
        } catch (ParseException e) {
            return SPECIAL_DATE;
        }
    }

    public String getAsString(FacesContext context, UIComponent component, Object value)
        throws ConverterException
    {
        return ObjectUtils.toString(value);
    }
}

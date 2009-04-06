/*
 * Created on Nov 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.openl.binding.IBindingContext;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author snshor
 *
 */
public class String2DataConvertorFactory
{
	
	static Locale locale = null;
	static final String localeCountry = "US", localeLang = "en";
	
	
	static Locale getLocale()
	{
		if (locale == null)
		{	
			String country =  System.getProperty("org.openl.locale.country");
			String lang = System.getProperty("org.openl.locale.lang");
			
			locale = new Locale(lang == null ? localeLang : lang, country == null ? localeCountry : country);
		}	
		
		return locale;
	}

	static HashMap<Class<?>,IString2DataConvertor> convertors;

	public static IString2DataConvertor getConvertor(Class<?> clazz) {
		IString2DataConvertor convertor = convertors.get(clazz);

		if (convertor == null) {
    		if (clazz.isEnum()){
    		    convertor = new String2EnumConvertor(clazz);
    		} else {
    		    try {
    		        Constructor<?> ctr = clazz.getDeclaredConstructor(new Class[]{String.class});
    		        convertor =  new String2ConstructorConvertor(ctr);
    		    } catch (Throwable t) {
    		        throw new RuntimeException("Convertor or Public Constructor " + clazz.getName() + "(String s) does not exist");
    		    }
    		}
		}
		
		return convertor;

	}



	static public void registerConvertor(Class<?> clazz, IString2DataConvertor conv)
	{
		convertors.put(clazz, conv);
	}
	
	
	
	public static class String2ConstructorConvertor implements IString2DataConvertor
	{
		
		Constructor<?> ctr;
		
		String2ConstructorConvertor(Constructor<?> ctr)
		{
			this.ctr = ctr;
		}
		
		public Object parse(String data, String format, IBindingContext cxt)
		{
			
			try
			{
				return ctr.newInstance(new Object[]{data});
			}
			catch (Exception e)
			{
				throw RuntimeExceptionWrapper.wrap(e);
			}
		}
		/**
		 *
		 */

		public String format(Object data, String format)
		{
			return String.valueOf(data);
		}

	}
	



	//Initialize 

	static {
		convertors = new HashMap<Class<?>, IString2DataConvertor>();

		convertors.put(int.class, new String2IntConvertor());
		convertors.put(double.class, new String2DoubleConvertor());
		convertors.put(char.class, new String2CharConvertor());
		convertors.put(boolean.class, new String2BooleanConvertor());
		convertors.put(long.class, new String2LongConvertor());


		convertors.put(Integer.class, new String2IntConvertor());
		convertors.put(Double.class, new String2DoubleConvertor());
		convertors.put(Character.class, new String2CharConvertor());
		convertors.put(Boolean.class, new String2BooleanConvertor());
		convertors.put(Long.class, new String2LongConvertor());



		convertors.put(String.class, new String2StringConvertor());
		convertors.put(Date.class, new String2DateConvertor());
		convertors.put(Calendar.class, new String2CalendarConvertor());
		convertors.put(Class.class, new String2ClassConvertor());
		convertors.put(IOpenClass.class, new String2OpenClassConvertor());
	}

	public static class String2LongConvertor implements IString2DataConvertor
	{
		
		public Object parse(String data, String format, IBindingContext cxt)
		{
			if (format == null)
				return Long.valueOf(data);
			DecimalFormat df = new DecimalFormat(format);
			
			 Number n;
			try
			{
				n = df.parse(data);
			}
			catch (ParseException e)
			{
				throw RuntimeExceptionWrapper.wrap(e);
			}
			 
			 return new Long(n.longValue());	
		}
		/**
		 *
		 */

		public String format(Object data, String format)
		{
			if (format == null)
				return String.valueOf(data);
			DecimalFormat df = new DecimalFormat(format);
			return 	df.format(((Long)data).intValue());
		}

	}




	public static class String2IntConvertor implements IString2DataConvertor
	{
		
		public Object parse(String data, String format, IBindingContext cxt)
		{
			if (format == null)
				return Integer.valueOf(data);
			DecimalFormat df = new DecimalFormat(format);
			
			 Number n;
			try
			{
				n = df.parse(data);
			}
			catch (ParseException e)
			{
				throw RuntimeExceptionWrapper.wrap(e);
			}
			 
			 return new Integer(n.intValue());	
		}
		/**
		 *
		 */

		public String format(Object data, String format)
		{
			if (format == null)
				return String.valueOf(data);
			DecimalFormat df = new DecimalFormat(format);
			return 	df.format(((Integer)data).intValue());
		}

	}

	public static class String2DoubleConvertor implements IString2DataConvertor
	{
		
		public Object parse(String xdata, String format, IBindingContext cxt)
		{
			
			if (format != null)
			{
				DecimalFormat df = new DecimalFormat(format);
				try
				{
					Number n = df.parse(xdata);
					
					return new Double(n.doubleValue());
				}
				catch (ParseException e)
				{
					throw RuntimeExceptionWrapper.wrap("", e);
				}
			}
			
			
			String data = numberStringWithoutModifier(xdata);
			
			double d = Double.parseDouble(data);
			
			return xdata == data ? new Double(d) : new Double(d * numberModifier(xdata));
		}
		/**
		 *
		 */

		public String format(Object data, String format)
		{
			if (format == null)
			{
				format = "#0.00";
			}
			
			DecimalFormat df = new DecimalFormat(format);
			
			return df.format(((Number)data).doubleValue());
		}

	}
	
	
	
	static String numberStringWithoutModifier(String s)
	{
		if (s.endsWith("%"))
		  return s.substring(0, s.length() - 1);
		  
		return s;  
	}
	
	static double numberModifier(String s)
	{
		if (s.endsWith("%"))
		  return 0.01;
		  
		return 1;  
	}
	
	
	

	public static class String2CharConvertor implements IString2DataConvertor
	{
		
		public Object parse(String data, String format, IBindingContext cxt)
		{
			if (data.length() != 1)
			  throw new IndexOutOfBoundsException("Character field must have only one symbol");
			
			return new Character(data.charAt(0));
		}

		public String format(Object data, String forma)
		{
			return new String(
			   new char[]{((Character)data).charValue()})
			;
		}


	}
	
	
	

	public static class String2BooleanConvertor implements IString2DataConvertor
	{
		
		public Object parse(String data, String format, IBindingContext cxt)
		{
			if (data == null || data.length() == 0)
			  return Boolean.FALSE;
			
			String lcase = data.toLowerCase().intern();
			
			if (lcase == "true" || lcase == "yes" || lcase == "t" || lcase == "y") 
				return Boolean.TRUE;
			
			if (lcase == "false" || lcase == "no" || lcase == "f" || lcase == "n") 
					return Boolean.FALSE;
			
			throw new RuntimeException("Invalid boolean value: " + data);
		}
		/**
		 *
		 */

		public String format(Object data, String format)
		{
			return String.valueOf(data);
		}

	}
	
    public static class String2EnumConvertor implements IString2DataConvertor {
        private Class<? extends Enum<?>> enumType;

        @SuppressWarnings("unchecked")
        public String2EnumConvertor(Class<?> clazz) {
            assert clazz.isEnum();
            enumType = (Class<? extends Enum<?>>) clazz;
        }

        public Object parse(String data, String format, IBindingContext cxt) {
            Enum<?> resolvedConstant = null;
            
            for (Enum<?> enumConstant : enumType.getEnumConstants()) {
                if (data.equalsIgnoreCase(enumConstant.name())) {
                    resolvedConstant = enumConstant;
                    break;
                }
            }

            if (resolvedConstant == null) {
                throw new RuntimeException(String.format(
                        "Constant corresponding to value \"%s\" can't be found in Enum %s ", data, enumType.getName()));
            }

            return resolvedConstant;
        }

        public String format(Object data, String format) {
            // Enum can override toString() method to display user-friendly
            // values
            return parse(String.valueOf(data), format, null).toString();
        }
    }
	
	
	public static class String2ClassConvertor implements IString2DataConvertor
	{
		
		public Object parse(String data, String format, IBindingContext cxt)
		{
			IOpenClass c =  cxt.findType(ISyntaxConstants.THIS_NAMESPACE, data);
			
			if (c == null)
				throw new RuntimeException("Type " + data + " is not found");

			return c.getInstanceClass();
		}
		/**
		 *
		 */

		public String format(Object data, String format)
		{
			return String.valueOf(data);
		}

	}
	
	public static class String2OpenClassConvertor implements IString2DataConvertor
	{
		
		public Object parse(String data, String format, IBindingContext cxt)
		{
			IOpenClass c =  cxt.findType(ISyntaxConstants.THIS_NAMESPACE, data);
			
			if (c == null)
				throw new RuntimeException("Type " + data + " is not found");

			return c;
		}

		/**
		 *
		 */

		public String format(Object data, String format)
		{
			return String.valueOf(data);
		}

	}	
	
	public static class String2StringConvertor implements IString2DataConvertor
	{
		
		public Object parse(String data, String format, IBindingContext cxt)
		{
			return data;
		}

		public String format(Object data, String format)
		{
			return String.valueOf(data);
		}

	}
	

	static public class String2DateConvertor implements IString2DataConvertor
	{
		
		DateFormat defaultFormat = DateFormat.getDateInstance(DateFormat.SHORT, getLocale());
		
		
		public Object parse(String data, String format,  IBindingContext cxt)
		{
			return parseDate(data, format);
		}
		
		public Date parseDate(String data, String format)
		{
			
			DateFormat df = format == null ? defaultFormat : new SimpleDateFormat(format, getLocale());
			
			
			try
			{
				return df.parse(data);
			}
			catch (ParseException e)
			{
				try
				{
					int value = Integer.parseInt(data);
					Calendar cc = Calendar.getInstance();
					cc.set(1900, 0, 1);
					cc.add(Calendar.DATE, value - 1);
					return cc.getTime();
					
				}
				catch(Throwable t)
				{
				}
				throw RuntimeExceptionWrapper.wrap(e);
			}
		}
		/**
		 *
		 */

		public String format(Object data, String format)
		{
				DateFormat df = format == null ? DateFormat.getDateInstance(DateFormat.SHORT): new SimpleDateFormat(format);
				return df.format(data);
		}

	}
	
	
	static public class String2CalendarConvertor implements IString2DataConvertor
	{
		
		
		
		public Object parse(String data, String format,  IBindingContext cxt)
		{
			return parseCalendar(data, format);
		}
		
		public Calendar parseCalendar(String data, String format)
		{
			
			Date d = new String2DateConvertor().parseDate(data, format);
			
			Calendar c = Calendar.getInstance(getLocale());
			
			c.setTime(d);
			
			return c;
			
		}
		/**
		 *
		 */

		public String format(Object data, String format)
		{
			return new String2DateConvertor().format(((Calendar)data).getTime(), format);
		}

	}
	
	
	
	
	
}

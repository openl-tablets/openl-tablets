package com.exigen.le.datalogger;

import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exigen.le.beangenerator.BeanGenerator;
import com.exigen.le.beangenerator.BeanTreeGenerator;
import com.exigen.le.beangenerator.GeneratorClassLoader;
import com.exigen.le.beangenerator.HolderToBeanConverter;
import com.exigen.le.project.VersionDesc;
import com.exigen.le.servicedescr.evaluator.BeanWrapper;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.accessor.ValueHolder;

public class DataLogger {
	
	private XMLEncoder encoder;
	private Map<String, Class<?>>  loadedClasses = new HashMap<String, Class<?>>();
	private GeneratorClassLoader cl = new GeneratorClassLoader();
	
	
	
	private DataLogger(){};
	
	public static DataLogger createInstance(String logFileName){
		
		DataLogger result = new DataLogger();
//		try {
//			result.cl.loadClass("java.beans.XMLEncoder");
//		} catch (ClassNotFoundException cnfe){
//			System.out.println("Sorry");
//		}
		try {
			result.setEncoder(new XMLEncoder(new BufferedOutputStream( new FileOutputStream(logFileName))));
		}	
		catch (FileNotFoundException fnfe){
			throw new RuntimeException("failed to created data log", fnfe);
		}
		
		return result;
		
	}
	
	private void setEncoder(XMLEncoder encoder){
		this.encoder = encoder;
	}
	
	private void sanityCheck(){
		if (encoder == null){
			throw new RuntimeException("DataLogger not initiated");
		}
	}
	
	/*
	 * Stop 
	 */
	public void close(){
		if (encoder != null){
			encoder.close();
		}
		encoder = null; // should not be used after that
	}
	
	/*
	 * Serialize objects
	 */
	public void write(Object[] olist){
		sanityCheck();
		for (Object o: olist){
			if (o instanceof BeanWrapper){ // bean - simply serialize
				encoder.writeObject(((BeanWrapper)o).getHolder());
			} else if (o instanceof ValueHolder){
				// value holder but not a bean wrapper - need to generate class for it
				ValueHolder vh = (ValueHolder)o;
				Type type = vh.getModel();
				String className = BeanGenerator.getQualifiedType(type);
				Class<?> clazz = loadedClasses.get(className);
				if (clazz == null){
					// define and load class
					clazz = BeanTreeGenerator.loadBeanClasses(className, type, cl);
					loadedClasses.put(className, clazz);
				}
				// copy from value holder to bean
				try {
					Object bean = HolderToBeanConverter.convert(type, vh, cl);
					ClassLoader old = Thread.currentThread().getContextClassLoader();
					Thread.currentThread().setContextClassLoader(cl);
					encoder.writeObject(bean);
					Thread.currentThread().setContextClassLoader(old);
				} catch (Exception e){
					throw new RuntimeException("failed to convert to bean", e);
				}
			} else if (Type.Primary.BOOLEAN.getJavaClass().isInstance(o)){
				BooleanBean b = new BooleanBean(); 
				b.setValue((Boolean)o);
				encoder.writeObject(b);
			} else if (Type.Primary.DATE.getJavaClass().isInstance(o)){
				CalendarBean c = new CalendarBean();
				c.setValue((Calendar)o);
				encoder.writeObject(c);
			} else if (Type.Primary.DOUBLE.getJavaClass().isInstance(o)){
				DoubleBean d = new DoubleBean();
				d.setValue((Double)o);
				encoder.writeObject(d);
			} else if (Type.Primary.STRING.getJavaClass().isInstance(o)){
				StringBean s = new StringBean();
				s.setValue((String)o);
				encoder.writeObject(s);
			}	
			
			else {
				throw new RuntimeException("Object type not supported: " + o.getClass());
			}
		}
		encoder.flush();
	}
	
	static class BooleanBean {
		Boolean value;

		public Boolean getValue() {
			return value;
		}

		public void setValue(Boolean value) {
			this.value = value;
		}
	}
	
	static class DoubleBean {
		Double value;

		public Double getValue() {
			return value;
		}

		public void setValue(Double value) {
			this.value = value;
		}
	}
	
	static class StringBean {
		String value;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}
	
	static class CalendarBean {
		Calendar value;

		public Calendar getValue() {
			return value;
		}

		public void setValue(Calendar value) {
			this.value = value;
		}
	}
	
}

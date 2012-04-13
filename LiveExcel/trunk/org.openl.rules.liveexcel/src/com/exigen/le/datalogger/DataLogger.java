package com.exigen.le.datalogger;

import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Calendar;

import com.exigen.le.servicedescr.evaluator.BeanWrapper;
import com.exigen.le.smodel.Primary;
import com.exigen.le.smodel.accessor.ValueHolder;

public class DataLogger {
	
	private XMLEncoder encoder;
	
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
			    //TODO
			} else if (Primary.BOOLEAN.getJavaClass().isInstance(o)){
				BooleanBean b = new BooleanBean(); 
				b.setValue((Boolean)o);
				encoder.writeObject(b);
			} else if (Primary.DATE.getJavaClass().isInstance(o)){
				CalendarBean c = new CalendarBean();
				c.setValue((Calendar)o);
				encoder.writeObject(c);
			} else if (Primary.DOUBLE.getJavaClass().isInstance(o)){
				DoubleBean d = new DoubleBean();
				d.setValue((Double)o);
				encoder.writeObject(d);
			} else if (Primary.STRING.getJavaClass().isInstance(o)){
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

package org.openl.tablets;

import org.openl.tablets.OpenLwithLEExample_Wrapper;


public class ExampleMain {

	//Creates new instance of Java Wrapper for our lesson
	static OpenLwithLEExample_Wrapper wrapper = new OpenLwithLEExample_Wrapper();

	public static void main(String[] args) 
	{
		Object result = wrapper.testLE();
		System.out.println(result);
	}
}

package org.openl.tablets.tutorial2;

import org.openl.rules.helpers.IntRange;
import org.openl.tablets.tutorial2.step3.Customer2_3;
import org.openl.types.impl.DynamicObject;



/**
 * @author snshor
 *
 * Run this class "as Java Application". As you progress with tutorial 
 * uncomment appropriate code.
 * 
 * This tutorial demonstarates data access using OpenL. As you can see all the data
 * entered as Excel tables is available to Java program via
 * standard java Beans methods returning arrays. 
 * 
 * No extra coding is required.
 *
 */



public class Tutorial2Main {

	//Creates new instance of Java Wrapper for our lesson
	static Tutorial_2Wrapper tut2 = new Tutorial_2Wrapper();

	public static void main(String[] args) 
	{
		
		
		
		// Step 1
		// Printing table pp1. Due to the fact that type Person1 has been
		// defined in OpenL it will appear in java as org.openl.types.impl.DynamicObject
		
		System.out.println("\n====== Step 1 =======\n");

		
		DynamicObject[] pp = tut2.getPp1();
		//pp = tut2.getPp11();
		for (int i = 0; i < pp.length; i++) {
			System.out.println(pp[i].getType().getName() + " " + (i+1) + ": " + pp[i]);
		}
		
		
		
		//Step 2
		// Printing Data Table "phrases21". 
		
		System.out.println("\n====== Step 2-1 =======\n");

		String[] phrases2 = tut2.getPhrases21();
		
		for (int i = 0; i < phrases2.length; i++) {
			System.out.println(phrases2[i]);
		}
		
		//Step 2-2
		// Printing Data Table "numbers22". 
		
		System.out.println("\n====== Step 2-2 =======\n");

		int[] numbers21 = tut2.getNumbers22();
		
		for (int i = 0; i < numbers21.length; i++) {
			System.out.println(numbers21[i]);
		}
		
		//Step 2-3
		// Printing Data Table "numbers22". 
		
		System.out.println("\n====== Step 2-2 =======\n");

		IntRange[] ranges23 = tut2.getRanges23();
		
		for (int i = 0; i < ranges23.length; i++) {
			System.out.println(ranges23[i]);
		}
		
	
		//Step 3
		// Printing Data Table "customers3".
		System.out.println("\n====== Step 3 =======\n");
		
		Customer2_3[] customers3 = tut2.getCustomers3();
		
		for (int i = 0; i < customers3.length; i++) {
			System.out.println(customers3[i].getName());
		}
		
		

	}
}

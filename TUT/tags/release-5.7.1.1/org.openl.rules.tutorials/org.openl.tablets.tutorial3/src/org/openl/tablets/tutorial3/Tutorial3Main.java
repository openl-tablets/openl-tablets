package org.openl.tablets.tutorial3;




/**
 * @author snshor
 *
 * Run this class "as Java Application". As you progress with tutorial 
 * uncomment appropriate code.
 * 
 * This tutorial will teach you more advanced features of decision and 
 * data tables. You will learn how to enter array data, use merged cells 
 * to structure tables with array elements, link multiple data tables 
 * into Object Graph using foreign keys-like approach, 
 * enter aggregate data objects as a single table, 
 * enter formulas to decision table cells. 
 * 
 */



public class Tutorial3Main {

	//Creates new instance of Java Wrapper for our lesson
	static Tutorial_3Wrapper tut3 = new Tutorial_3Wrapper();

	public static void main(String[] args) 
	{
		
		
		
		// Step 1
		// Converts AM/PM hour to 24-hour and vice-versa 
		
		System.out.println("\n====== Step 1 =======\n");

		
		int ampmHr = 11;
		String ampmStr = "PM";
		int hr24 = tut3.ampmTo24(ampmHr, ampmStr);
		
		System.out.println("Converted " + ampmHr + ampmStr + " to " + hr24 + " hours");
	
		
		
		String ampmtime = tut3.hr24ToAmpm(hr24);
		System.out.println("Converted " + hr24 + " hours to " + ampmtime);
		
		
		//Step 2
		// Defining state region 
		
		System.out.println("\n====== Step 2 =======\n");

		String state = "AK";
		String region = tut3.region(state);
		
		System.out.println(state + " is located in " + region + " region");
		
		//Step 2-1
		System.out.println("\n====== Step 2-1 =======\n");

		state = "NJ";
		region = tut3.region21(state);
		
		System.out.println(state + " is located in " + region + " region");
		
		//Step 2-2
		
		System.out.println("\n====== Step 2-2 =======\n");
		state = "IA";
		region = tut3.region22(state);
		
		System.out.println(state + " is located in " + region + " region");

		

		
		
	
		//Step 3
		// Printing Data Table "addresses3".
		System.out.println("\n====== Step 3 =======\n");
		
		Address[] addr3 = tut3.getAddresses3();
		for (int i = 0; i < addr3.length; i++) {
			System.out.println("--------------");
			System.out.println(addr3[i]);
		}
		
		//Step 3-1
		// Printing Data Table "addresses31".
		System.out.println("\n====== Step 3-1 =======\n");
		
		Address[] addr31 = tut3.getAddresses31();
		for (int i = 0; i < addr31.length; i++) {
			System.out.println("--------------");
			System.out.println(addr31[i]);
		}
		

	}
}

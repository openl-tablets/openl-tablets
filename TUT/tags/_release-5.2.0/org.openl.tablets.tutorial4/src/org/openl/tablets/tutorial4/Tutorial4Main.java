/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.tablets.tutorial4;

import org.openl.vm.Tracer;

/**
 * @author snshor
 *
 */
public class Tutorial4Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		Tracer.setTracer(new Tracer());
		Tutorial_4Wrapper tut4 = new Tutorial_4Wrapper();
		
		Driver d = new Driver();
		d.setAge(99);
		
		
		
		Tracer.getTracer().print(System.out);
		
	//	tut4.setDriverEligibility(d);
	}

}

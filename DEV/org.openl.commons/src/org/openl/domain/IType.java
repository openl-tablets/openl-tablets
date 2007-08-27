/*
 * Created on May 6, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.domain;

import org.openl.base.INameSpacedThing;
import org.openl.base.NameSpacedThing;


/**
 * @author snshor
 */
public interface IType extends INameSpacedThing
{
	
	/**
	 * @param obj
	 * @return true if obj belongs to this type
	 * 
	 * Please note how it is similar to selector or domain methods
	 * 
	 */
	
	boolean isTypeFor(Object obj);
	
	/**
	 * 
	 * @param type 
	 * @return true if this is specialization of more general type
	 * if (T1.isKindOf(T2) AND T1.isTypeFor(x)) -> T2.isTypeFor(x) 
	 */
	
	boolean isKindOf(IType type);
	
	
	static public final AnyThing ANY = new AnyThing(); 
 	
	static public class AnyThing extends NameSpacedThing implements IType
	{
		
		private AnyThing()
		{
			super("Any", "http://domain.openl.org");
		}
		
		public boolean isTypeFor(Object obj)
		{
			return true;
		}

		public boolean isKindOf(IType type)
		{
			return type == this;
		}
	}
	
}

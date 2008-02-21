/**
 * Created Aug 23, 2007
 */
package org.openl.rules.dt;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;

/**
 * @author snshor
 *
 */
public class BooleanTypeAdaptor
{
	static final String booleanValue = "booleanValue";
	
	static public BooleanTypeAdaptor getAdaptor(IOpenClass ioc)
	{
		if (ioc.getInstanceClass() == boolean.class || 
				ioc.getInstanceClass() == Boolean.class)
			return new BooleanTypeAdaptor();
		
		IOpenMethod bv = ioc.getMatchingMethod(booleanValue, IOpenClass.EMPTY);
		if (bv != null)
			return new BoolMethodAdaptor(bv);

		IOpenField bf = ioc.getField(booleanValue, true);
		if (bf != null)
			return new BoolFieldAdaptor(bf);
		
		return null;
	}
	
	public boolean extractBooleanValue(Object bool)
	{
		return ((Boolean)bool).booleanValue();
	}
	
	static class BoolMethodAdaptor extends BooleanTypeAdaptor
	{
		IOpenMethod bv;

		/**
		 * @param bv
		 */
		public BoolMethodAdaptor(IOpenMethod bv)
		{
			this.bv = bv;
		}

		public boolean extractBooleanValue(Object bool)
		{
			return ((Boolean)bv.invoke(bool, new Object[0], null)).booleanValue();
		}
		
	}

	static class BoolFieldAdaptor extends BooleanTypeAdaptor
	{
		IOpenField bv;

		/**
		 * @param bv
		 */
		public BoolFieldAdaptor(IOpenField bv)
		{
			this.bv = bv;
		}

		public boolean extractBooleanValue(Object bool)
		{
			return ((Boolean)bv.get(bool, null)).booleanValue();
		}
		
	}
	
	
}

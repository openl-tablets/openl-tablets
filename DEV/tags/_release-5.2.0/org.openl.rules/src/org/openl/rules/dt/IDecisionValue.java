/*
 * Created on Nov 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.rules.dt;

/**
 * @author snshor
 *
 */
public interface IDecisionValue
{

	static final public int 
	  
	  UNDEFINED = 0,
	  FALSE = 1,
	  TRUE = 2,
	  NA = 3,
	  SPECIAL_FALSE = 4,
	  SPECIAL_TRUE = 5;


	int getType();
	
	boolean getBooleanValue();
	
	boolean isSpecial();
	
	
	
	
	
	static final public IDecisionValue
	  Undefined = new Value(UNDEFINED, true, true),
	  
	  False = new Value(FALSE, false, false),
	  True = new Value(TRUE, true, false),
	  NxA = new Value(NA, true, true),
	  SpecialFalse = new Value(SPECIAL_FALSE, false, true),
	  SpecialTrue = new Value(SPECIAL_TRUE, true, true)
	  ;
	
	static public class Value implements IDecisionValue
	{
		int type;
		boolean booleanValue;
		
		boolean special;

		
		public Value(int type, boolean booleanValue, boolean special)
		{
			this.type = type;
			this.booleanValue = booleanValue;
			this.special = special;
		}

		
		
		/**
		 * @return
		 */
		public boolean getBooleanValue()
		{
			return booleanValue;
		}

		/**
		 * @return
		 */
		public int getType()
		{
			return type;
		}


		/**
		 * @return
		 */
		public boolean isSpecial()
		{
			return special;
		}

	}
	
}

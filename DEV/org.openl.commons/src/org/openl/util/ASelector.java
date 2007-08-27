/*
 * Created on May 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

/**
 * @author snshor
 *
 */
public abstract class ASelector implements ISelector
{

	static public ISelector selectClass(Class c)
	{
		return new ClassSelector(c);
	}

	static public ISelector selectObject(Object obj)
	{
		return new ObjectSelector(obj);
	}

	/* (non-Javadoc)
	 * @see org.openl.util.ISelector#or(org.openl.util.ISelector)
	 */
	public ISelector or(ISelector isel)
	{
		return new ORSelector(this, isel);
	}

	/* (non-Javadoc)
	 * @see org.openl.util.ISelector#and(org.openl.util.ISelector)
	 */
	public ISelector and(ISelector isel)
	{
		return new ANDSelector(this, isel);
	}

	/* (non-Javadoc)
	 * @see org.openl.util.ISelector#xor(null)
	 */
	public ISelector xor(ISelector isel)
	{
		return new XORSelector(this, isel);
	}

	/**
	 * 
	 * @author snshor
	 * Base class for binary boolean operators
	 */
	static abstract class BoolBinSelector extends ASelector
	{
		ISelector sel1;
		ISelector sel2;
		protected BoolBinSelector(ISelector sel1, ISelector sel2)
		{
			this.sel1 = sel1;
			this.sel2 = sel2;
		}

		protected boolean equalsSelector(ASelector sel)
		{
			return sel1.equals(((BoolBinSelector) sel).sel1)
				&& sel2.equals(((BoolBinSelector) sel).sel2);
		}

		protected int redefinedHashCode()
		{
			return sel1.hashCode() + sel2.hashCode();
		}

	}

	static class NOTSelector extends ASelector
	{
		ISelector is;
		public NOTSelector(ISelector is)
		{
			this.is = is;
		}
		public boolean select(Object obj)
		{
			return !is.select(obj);
		}

		protected boolean equalsSelector(ASelector sel)
		{
			return is.equals(((NOTSelector) sel).is);
		}

		protected int redefinedHashCode()
		{
			return is.hashCode();
		}

	}

	static class XORSelector extends BoolBinSelector
	{
		public XORSelector(ISelector sel1, ISelector sel2)
		{
			super(sel1, sel2);
		}

		public boolean select(Object obj)
		{
			return sel1.select(obj) ^ sel2.select(obj);
		}
	}

	static class ANDSelector extends BoolBinSelector
	{
		public ANDSelector(ISelector sel1, ISelector sel2)
		{
			super(sel1, sel2);
		}

		public boolean select(Object obj)
		{
			if (sel1.select(obj))
				return sel2.select(obj);
			return false;
		}
	}

	static class ORSelector extends BoolBinSelector
	{
		public ORSelector(ISelector sel1, ISelector sel2)
		{
			super(sel1, sel2);
		}

		public boolean select(Object obj)
		{
			if (sel1.select(obj))
				return true;
			return sel2.select(obj);
		}
	}

	public static class ClassSelector extends ASelector
	{
		Class c;
		public ClassSelector(Class c)
		{
			this.c = c;
		}

		public boolean select(Object obj)
		{
			return c.isInstance(obj);
		}
		
		
		
		
		protected boolean equalsSelector(ASelector sel)
		{
			return c == ((ClassSelector)sel).c;
		}

		protected int redefinedHashCode()
		{
			return c.hashCode();
		}

	}

	public static class ObjectSelector extends ASelector
	{
		Object myobj;
		public ObjectSelector(Object obj)
		{
			this.myobj = obj;
		}

		public boolean select(Object obj)
		{
			if (myobj == obj)
				return true;
			if (myobj == null)
				return false;
			return myobj.equals(obj);
		}

		protected boolean equalsSelector(ASelector sel)
		{
			return select(((ObjectSelector)sel).myobj);
		}

		protected int redefinedHashCode()
		{
			return myobj == null ? 0 : myobj.hashCode();
		}


	}

	public static abstract class IntValueSelector extends ASelector
	{
		int value;
		public IntValueSelector(int value)
		{
			this.value = value;
		}

		public boolean select(Object obj)
		{
			return value == getIntValue(obj);
		}

		protected abstract int getIntValue(Object test);

		protected boolean equalsSelector(ASelector sel)
		{
			return ((IntValueSelector)sel).value == value;
		}

		protected int redefinedHashCode()
		{
			return value;
		}
		
		
	}

	public static class StringValueSelector extends ASelector
	{
		String value;
		AStringConvertor convertor;
		
		public StringValueSelector(String value, AStringConvertor convertor)
		{
			this.value = value;
			this.convertor =convertor;
		}

		public boolean select(Object obj)
		{
			return value.equals(convertor.getStringValue(obj));
		}

        protected boolean equalsSelector(ASelector sel)
        {
          StringValueSelector svs = (StringValueSelector)sel;
          return value.equals(svs.value) && convertor.equals(svs.convertor);
        }

        protected int redefinedHashCode()
        {
            return value.hashCode() * 37 + convertor.hashCode();
        }
	}

	/* (non-Javadoc)
	 * @see org.openl.util.ISelector#not(org.openl.util.ISelector)
	 */
	public ISelector not()
	{
		return new NOTSelector(this);
	}

	public boolean equals(Object obj)
	{
		if (obj == null || obj.getClass() != this.getClass())
			return false;
		return equalsSelector((ASelector) obj);
	}

	protected boolean equalsSelector(ASelector sel){return sel == this;}

	protected int redefinedHashCode(){return System.identityHashCode(this);}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		return redefinedHashCode();
	}

}

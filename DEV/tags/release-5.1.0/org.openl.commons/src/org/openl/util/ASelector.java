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
public abstract class ASelector<T> implements ISelector<T>
{

    static public ISelector<Object> selectClass(Class<?> c)
    {
	return new ClassSelector(c);
    }

    static public <T> ISelector<T> selectObject(T obj)
    {
	return new ObjectSelector<T>(obj);
    }

    static public <T> ISelector<T> selectAll(@SuppressWarnings("unused")
    T obj)
    {
	return new AllSelector<T>();
    }

    static public <T> ISelector<T> selectNone(@SuppressWarnings("unused")
    T obj)
    {
	return new NoneSelector<T>();
    }

    static class AllSelector<T> extends ASelector<T>
    {

	public boolean select(T obj)
	{
	    return true;
	}

    }

    static class NoneSelector<T> extends ASelector<T>
    {

	public boolean select(T obj)
	{
	    return false;
	}

    }

    public ISelector<T> or(ISelector<T> isel)
    {
	return new ORSelector<T>(this, isel);
    }

    public ISelector<T> and(ISelector<T> isel)
    {
	return new ANDSelector<T>(this, isel);
    }

    public ISelector<T> xor(ISelector<T> isel)
    {
	return new XORSelector<T>(this, isel);
    }

    /**
     * 
     * @author snshor Base class for binary boolean operators
     */
    static abstract class BoolBinSelector<T> extends ASelector<T>
    {
	ISelector<T> sel1;
	ISelector<T> sel2;

	protected BoolBinSelector(ISelector<T> sel1, ISelector<T> sel2)
	{
	    this.sel1 = sel1;
	    this.sel2 = sel2;
	}

	protected boolean equalsSelector(ASelector<?> sel)
	{
	    return sel1.equals(((BoolBinSelector<?>) sel).sel1)
		    && sel2.equals(((BoolBinSelector<?>) sel).sel2);
	}

	protected int redefinedHashCode()
	{
	    return sel1.hashCode() + sel2.hashCode();
	}

    }

    static class NOTSelector<T> extends ASelector<T>
    {
	ISelector<T> is;

	public NOTSelector(ISelector<T> is)
	{
	    this.is = is;
	}

	public boolean select(T obj)
	{
	    return !is.select(obj);
	}

	protected boolean equalsSelector(ASelector<?> sel)
	{
	    return is.equals(((NOTSelector<?>) sel).is);
	}

	protected int redefinedHashCode()
	{
	    return is.hashCode();
	}

    }

    static class XORSelector<T> extends BoolBinSelector<T>
    {
	public XORSelector(ISelector<T> sel1, ISelector<T> sel2)
	{
	    super(sel1, sel2);
	}

	public boolean select(T obj)
	{
	    return sel1.select(obj) ^ sel2.select(obj);
	}
    }

    static class ANDSelector<T> extends BoolBinSelector<T>
    {
	public ANDSelector(ISelector<T> sel1, ISelector<T> sel2)
	{
	    super(sel1, sel2);
	}

	public boolean select(T obj)
	{
	    if (sel1.select(obj))
		return sel2.select(obj);
	    return false;
	}
    }

    static class ORSelector<T> extends BoolBinSelector<T>
    {
	public ORSelector(ISelector<T> sel1, ISelector<T> sel2)
	{
	    super(sel1, sel2);
	}

	public boolean select(T obj)
	{
	    if (sel1.select(obj))
		return true;
	    return sel2.select(obj);
	}
    }

    public static class ClassSelector extends ASelector<Object>
    {
	Class<?> c;

	public ClassSelector(Class<?> c)
	{
	    this.c = c;
	}

	public boolean select(Object obj)
	{
	    return c.isInstance(obj);
	}

	protected boolean equalsSelector(ASelector<?> sel)
	{
	    return c == ((ClassSelector) (ASelector<Object>)sel).c;
	}

	protected int redefinedHashCode()
	{
	    return c.hashCode();
	}

    }

    public static class ObjectSelector<T> extends ASelector<T>
    {
	T myobj;

	public ObjectSelector(T obj)
	{
	    this.myobj = obj;
	}

	public boolean select(T obj)
	{
	    if (myobj == obj)
		return true;
	    if (myobj == null)
		return false;
	    return myobj.equals(obj);
	}

	@SuppressWarnings("unchecked")
	protected boolean equalsSelector(ASelector<?> sel)
	{
	    return select(((ObjectSelector<T>) sel).myobj);
	}

	protected int redefinedHashCode()
	{
	    return myobj == null ? 0 : myobj.hashCode();
	}

    }

    public static abstract class IntValueSelector<T> extends ASelector<T>
    {
	int value;

	public IntValueSelector(int value)
	{
	    this.value = value;
	}

	public boolean select(T obj)
	{
	    return value == getIntValue(obj);
	}

	protected abstract int getIntValue(T test);

	protected boolean equalsSelector(ASelector<?> sel)
	{
	    return ((IntValueSelector<?>) sel).value == value;
	}

	protected int redefinedHashCode()
	{
	    return value;
	}

    }

    public static class StringValueSelector<T> extends ASelector<T>
    {
	String value;
	AStringConvertor<T> convertor;

	public StringValueSelector(String value, AStringConvertor<T> convertor)
	{
	    this.value = value;
	    this.convertor = convertor;
	}

	public boolean select(T obj)
	{
	    return value.equals(convertor.getStringValue(obj));
	}

	protected boolean equalsSelector(ASelector<?> sel)
	{
	    StringValueSelector<?> svs = (StringValueSelector<?>) sel;
	    return value.equals(svs.value) && convertor.equals(svs.convertor);
	}

	protected int redefinedHashCode()
	{
	    return value.hashCode() * 37 + convertor.hashCode();
	}
    }

    public ISelector<T> not()
    {
	return new NOTSelector<T>(this);
    }

    public boolean equals(Object obj)
    {
	if (obj == null || obj.getClass() != this.getClass())
	    return false;
	return equalsSelector((ASelector<?>) obj);
    }

    protected boolean equalsSelector(ASelector<?> sel)
    {
	return sel == this;
    }

    protected int redefinedHashCode()
    {
	return System.identityHashCode(this);
    }

    public int hashCode()
    {
	return redefinedHashCode();
    }

}

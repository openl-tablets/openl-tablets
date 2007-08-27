/*
 * Created on Apr 30, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.domain;

import java.util.BitSet;
import java.util.Iterator;

import org.openl.util.AOpenIterator;

/**
 * @author snshor
 */
public class EnumDomain // implements ISetDomain
{

	BitSet bits;
	Enum enumeration;

	public EnumDomain(Enum enumeration, Object[] objs)
	{
		bits = new BitSet(enumeration.size());
		this.enumeration = enumeration;

		for (int i = 0; i < objs.length; i++)
		{
			int idx = enumeration.getIndex(objs[i]);
			bits.set(idx);
		}
	}
	
	
	public EnumDomain(Object[] elements)
	{
		this(new Enum(elements), elements);
	}

	
	

	public EnumDomain(Enum enumeration, BitSet bits)
	{
		this.bits = bits;
		this.enumeration = enumeration;
	}

	/**
	 *
	 */

	public EnumDomain and(EnumDomain sd)
	{
		checkOperand(sd);

		//		if (!bits.intersects(sd.bits))
		//		{
		//			return enum.empty();
		//		}

		if (bits.equals(sd.bits))
			return this;

		BitSet copy = (BitSet) bits.clone();
		copy.and(sd.bits);
		return new EnumDomain(enumeration, copy);

	}

	void checkOperand(EnumDomain sd)
	{

		if (sd.getEnum() != enumeration)
			throw new RuntimeException("Can not use subsets of different domains");

	}

	/**
	 *
	 */

	public boolean contains(Object obj)
	{
		int idx = enumeration.getIndex(obj);
		return bits.get(idx);
	}

	/**
	 *
	 */

	public Iterator iterator()
	{
		return new EnumDomainIterator();
	}

	/**
	 *
	 */

	public EnumDomain not()
	{
		int size = enumeration.size();

		BitSet bs = (BitSet) bits.clone();

		bs.flip(0, size);

		return new EnumDomain(enumeration, bs);
	}

	/**
	 *
	 */

	public EnumDomain or(EnumDomain sd)
	{
		checkOperand(sd);

		if (bits.equals(sd.bits))
			return this;

		BitSet copy = (BitSet) bits.clone();
		copy.or(sd.bits);
		return new EnumDomain(enumeration, copy);
	}

	/**
	 *
	 */

	public EnumDomain sub(EnumDomain sd)
	{
		checkOperand(sd);

		if (bits.equals(sd.bits))
			return this;

		BitSet copy = (BitSet) bits.clone();
		copy.andNot(sd.bits);
		return new EnumDomain(enumeration, copy);
	}

	/**
	 * @return
	 */
	public Enum getEnum()
	{
		return enumeration;
	}

	class EnumDomainIterator extends AOpenIterator
	{

		BitSetIterator bsi = new BitSetIterator(bits);

		/**
			 *
			 */

		public boolean hasNext()
		{
			return bsi.hasNext();
		}

		/**
		 *
		 */

		public Object next()
		{
			int idx = bsi.nextInt();
			return enumeration.objs[idx];
		}

	}
	
	
	public int size()
	{
		return bits.cardinality();
	}
	

	/**
	 *
	 */

	public boolean equals(Object obj)
	{
		if (obj == null || !(obj instanceof EnumDomain))
		  return false;
		  
		EnumDomain ed = (EnumDomain)obj;
		
		return enumeration.equals(ed.enumeration) && bits.equals(ed.bits);  
		
	}

	/**
	 *
	 */

	public int hashCode()
	{
		return enumeration.hashCode() * 37 + bits.hashCode();
	}

}

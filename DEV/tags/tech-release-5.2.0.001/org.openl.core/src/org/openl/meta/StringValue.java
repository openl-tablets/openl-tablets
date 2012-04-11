package org.openl.meta;

import org.openl.IOpenSourceCodeModule;
import org.openl.syntax.impl.StringSourceCodeModule;

public class StringValue implements IMetaHolder, CharSequence,
	Comparable<StringValue>
{
    IMetaInfo metaInfo;
    String value;

    @Override
    public boolean equals(Object obj)
    {

	if (obj instanceof StringValue)
	{
	    StringValue v = (StringValue) obj;
	    return value.equals(v.value);
	}
	if (obj instanceof String)
	{
	    String s = (String) obj;
	    return value.equals(s);
	}

	return false;
    }

    @Override
    public int hashCode()
    {
	return value.hashCode();
    }

    public IMetaInfo getMetaInfo()
    {
	return metaInfo;
    }

    public void setMetaInfo(IMetaInfo metaInfo)
    {
	this.metaInfo = metaInfo;
    }

    public StringValue(String value)
    {
	if (value == null)
	    throw new NullPointerException();
	this.value = value;
    }

    public StringValue(String value, String shortName, String fullName,
	    String sourceUrl)
    {
	if (value == null)
	    throw new NullPointerException();
	this.value = value;
	metaInfo = new ValueMetaInfo(shortName, fullName, sourceUrl);
    }

    public String getValue()
    {
	return value;
    }

    public void setValue(String value)
    {
	this.value = value;
    }

    public IOpenSourceCodeModule asSourceCodeModule()
    {
	return new StringSourceCodeModule(value, getMetaInfo().getSourceUrl());
    }

    public String toString()
    {
	return value;
    }

    public char charAt(int index)
    {
	return value.charAt(index);
    }

    public int length()
    {
	return value.length();
    }

    public CharSequence subSequence(int start, int end)
    {
	return value.subSequence(start, end);
    }

    public int compareTo(StringValue v)
    {
	return value.compareTo(v.value);
    }

    public boolean isEmpty()
    {
	return value.trim().length() == 0;
    }

}

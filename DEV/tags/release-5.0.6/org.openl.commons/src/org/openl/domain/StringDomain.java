/**
 * Created Apr 8, 2007
 */
package org.openl.domain;

import org.openl.util.StringTool;

/**
 * @author snshor
 *
 */
public class StringDomain extends EnumDomain<String>
{
	
	public StringDomain(String[] elements)
	{
		super(elements);
	}
	
	
	public StringDomain(String src, String sep)
	{
		this(StringTool.tokenize(src, sep));
	}

	public StringDomain(String src)
	{
		this(src, ", ");
	}


	public StringDomain(Enum<String> enumeration, String[] objs)
	{
		super(enumeration, objs);
	}
	
}

/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.rules.indexer;

import java.util.Vector;

/**
 * @author snshor
 *
 */
public class Tokenizer
{

	String src;

	


	public Tokenizer(String src)
	{
		this.src = src;
	}

	
	
	
	
	int startToken = 0;
	int pos = 0;

	int len;
	
	public String[] parse()
	{
			Vector v = new Vector();
			len = src.length();
			
			
			
			
			for (; pos < len; pos++)
			{
				char c = src.charAt(pos);
				if (Character.isLetter(c))
				{
					v.add(getAlphanumericToken());
				}
				else if (Character.isDigit(c))
				{
					v.add(getNumberToken());
				}					
			}
			
			return (String[]) v.toArray(new String[v.size()]);
			
		
	}


	private Object getNumberToken()
	{
		startToken = pos;
		++pos;
		
		for (; pos < len; pos++)
		{
			char c = src.charAt(pos);
			if (Character.isDigit(c) || c == '.' || c == '%')
				continue;
			return src.substring(startToken, pos);
		}
		
		return src.substring(startToken, pos);
	}


	private String getAlphanumericToken()
	{
		startToken = pos;
		++pos;
		
		for (; pos < len; pos++)
		{
			char c = src.charAt(pos);
			if (Character.isLetterOrDigit(c) || c == '_' || c == '#' || c == '&')
				continue;
			return src.substring(startToken, pos);
		}
		
		return src.substring(startToken, pos);
	}


}

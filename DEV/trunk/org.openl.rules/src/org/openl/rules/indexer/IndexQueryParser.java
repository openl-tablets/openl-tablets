package org.openl.rules.indexer;

import java.util.Vector;

public class IndexQueryParser
{

	Vector included = new Vector();
	Vector excluded = new Vector();
	
	String input;
	
	String[] tokens = new String[100];
	int nTokens =0;
	int start = 0;
	boolean T = false;
	boolean Q = false;
	boolean N = false;
	boolean t = false;
	boolean s = false;
	boolean q = false;
	boolean n = false;
	
	int pos = 0;
	 
	public IndexQueryParser(String input)
	{
		this.input = input;
	}

	void flushTokens()
	{
		if (nTokens == 0)
			return;
		
		String[] newtokens = new String[nTokens];
		for (int i = 0; i < nTokens; i++)
		{
			newtokens[i] = tokens[i];
		}
		
		(N ? excluded : included ).add(newtokens);
		N = Q = T = false;
		nTokens = 0;
	}
	
	void newToken()
	{
		start = pos;
		T = true;
	}
	
	void closeToken()
	{
		if (!T) return;
		tokens[nTokens++] = input.substring(start, pos);
		T = false;
	}
	
	
	
	public IndexQuery parse()
	{
		
		for (; pos < input.length(); pos++)
		{
			n=t=s=q=false;
			char c = input.charAt(pos);
			
			if (c == '-')
				n = true;
			else if (c == '"')
				q = true;
			else if (c == ' ')
				s = true;
			else t = true;
				
			if (!T && t)
				newToken();
			else if (!Q && T && s)
			{
				closeToken();
				flushTokens();
			}
			else if (Q && T && s)
				closeToken();
			else if (!Q && q)
			{
				closeToken();
				flushTokens();
				Q = true;
			}
			else if (Q && q)
			{
				closeToken();
				flushTokens();
				Q = false;
			}
			else if (n)
				N = true;
		}
		
		closeToken();
		flushTokens();
		
		IndexQuery iq = new IndexQuery((String[][])included.toArray(new String[0][]), (String[][])excluded.toArray(new String[0][]), null);

		return iq;
	}
	
/*
 Q  T  q  t  s  n  N    Q nt ct f
    N     Y                x
 N  Y        Y                x  x
 Y  Y        Y                x 
 N     Y                t     x  x
 Y     Y                f     x  x
                Y  t          x  x     
 */
}

package org.openl.rules.indexer;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


public class Index
{

	Map firstCharMap = new TreeMap();
	
	
 
	public void add(String token, IIndexElement element)
	{
		
		
		TokenBucket tb = findOrCreateTokenBucket(token);
		
		tb.addIndexElement(token, element);
		
	}

	final static String[] suffixes = {"ies", "es", "s", "ied", "ed", "id","y"};
	

	static String[][] exceptions_array = {{"s", "was", "whereas", "us"},
		{"d", "word"},
		{"es", "yes"},
		{"id", "_id"}
		 };
	
	static boolean isException(String suffix, String lc)
	{
		for (int i = 0; i < exceptions_array.length; i++)
		{
			if (exceptions_array[i][0].equals(suffix))
			  for (int j = 1; j < exceptions_array[i].length; j++)
				{
					if (lc.endsWith(exceptions_array[i][j]))
						return true;
				} 	
		}
		
		return false;
	}
	
	
	static String getRoot(String token)
	{
		String lc = token.toLowerCase();
		
		int len = token.length();
		for (int i = 0; i < suffixes.length; i++)
		{
			if (len > suffixes[i].length() && lc.endsWith(suffixes[i]))
			{
//				if (!isException(suffixes[i], lc))
				  return lc.substring(0, len - suffixes[i].length());
//				return lc;
			}	
		}
		
		return lc;
	}
	
	
	protected synchronized TokenBucket getTokenBucket(Map tokenMap, String token)
	{
		TokenBucket tb = (TokenBucket)tokenMap.get(token);
		if (tb == null)
		{	
		  tb = new TokenBucket(token);
		  tokenMap.put(token, tb);
		}  
		return tb;
	}




	protected synchronized Map getFirstCharMap(String charStr)
	{
		Map map = (Map)firstCharMap.get(charStr);
		if (map == null)
		{	
		  map = new TreeMap();
		  firstCharMap.put(charStr, map);
		}  
		return map;
	}




	static public class TokenBucket
	{
		TokenBucket(String token)
		{
			this.baseToken = token;
		}
		
		public void addIndexElement(String token, IIndexElement element)
		{
			tokens.put(token, token);
			
			getHitBucket(element).inc();
		}
		
		
		public synchronized HitBucket getHitBucket(IIndexElement element)
		{
			String uri = element.getUri();
			HitBucket hb = (HitBucket) indexElements.get(uri);
			if (hb == null)
			{
				hb = new HitBucket(element);
				indexElements.put(uri, hb);
			}	
			return hb;
				
		}
		
		String baseToken;
//		Map indexElements = new TreeMap();
//		Map tokens = new TreeMap(TOKEN_COMPARATOR);
		Map indexElements = new HashMap();
		Map tokens = new HashMap();
		
		static public final TokenComparator TOKEN_COMPARATOR = new TokenComparator();
		
		public int size()
		{
			return indexElements.size();
		}

		public String getBaseToken()
		{
			return baseToken;
		}

		public Map getIndexElements()
		{
			return indexElements;
		}

		public Map getTokens()
		{
			return tokens;
		}
		
		public String displayValue()
		{
			return (String)tokens.values().iterator().next();
		}
		
		
		static class TokenComparator implements Comparator
		{

			public int compare(Object arg0, Object arg1)
			{
				String s0 = (String)arg0;
				String s1 = (String)arg1;
				return s0.length() == s1.length() ? s0.compareTo(s1) : s0.length() - s1.length() ;
			}
			
		}
		
	}




	public Map getFirstCharMap()
	{
		return firstCharMap;
	}


	public TokenBucket findOrCreateTokenBucket(String token)
	{
		String charStr = token.substring(0,1).toUpperCase();
		
		Map charMap = getFirstCharMap(charStr);
		
		String tokenRoot = getRoot(token);
		TokenBucket tb = getTokenBucket(charMap, tokenRoot);
		return tb;
	}
	
	
	public TokenBucket findTokenBucket(String token)
	{
		String charStr = token.substring(0,1).toUpperCase();

		Map charMap = (Map)firstCharMap.get(charStr);
		
		if (charMap == null)
			return null;
		
		
		String tokenRoot = getRoot(token);
		
		TokenBucket tb = (TokenBucket)charMap.get(tokenRoot);
		
		return tb;
	}
	
	
	
	
	
}

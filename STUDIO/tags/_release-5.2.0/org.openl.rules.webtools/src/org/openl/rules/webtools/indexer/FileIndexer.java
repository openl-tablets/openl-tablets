

package org.openl.rules.webtools.indexer;



import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.openl.main.SourceCodeURLTool;
import org.openl.rules.indexer.DefaultIndexer;
import org.openl.rules.indexer.HitBucket;
import org.openl.rules.indexer.IIndexElement;
import org.openl.rules.indexer.IIndexParser;
import org.openl.rules.indexer.IIndexer;
import org.openl.rules.indexer.Index;
import org.openl.rules.indexer.IndexQuery;
import org.openl.rules.indexer.IndexQueryParser;
import org.openl.rules.indexer.IndexRunner;
import org.openl.rules.indexer.TableIndexParser;
import org.openl.rules.indexer.WordDocIndexParser;
import org.openl.rules.indexer.WorkbookIndexParser;
import org.openl.rules.indexer.WorksheetIndexParser;
import org.openl.rules.indexer.Index.TokenBucket;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.syntax.XlsURLConstants;
import org.openl.rules.webtools.WebTool;
import org.openl.rules.word.WordDocSourceCodeModule;
import org.openl.syntax.impl.FileSourceCodeModule;
import org.openl.util.Log;
import org.openl.util.StringTool;

/**
 * @author snshor
 *
 */
public class FileIndexer extends WebTool
{
	long[] updateTimes;
	
	String[] files = null;
	
	
	
	
	boolean isFilesChanged(String[] xfiles, long[] times)
	{
		if (files == null)
			return true;
		if (files.length != xfiles.length)
			return true;
	
		for (int i = 0; i < xfiles.length; i++)
		{
			if (!xfiles[i].equals(files[i]))
				return true;
		}
		
		if (updateTimes == null)
			return true;
		
		for (int i = 0; i < times.length; i++)
		{
			if (times[i] != updateTimes[i])
				return true;
		}
		return false;
	}
	
	
	
	static long[] makeTimes(String[] xfiles)
	{
		long[] times = new long[xfiles.length];
		
		for (int i = 0; i < xfiles.length; i++)
		{
			times[i] = new File(xfiles[i]).lastModified();
		}
		return times;
	}
	
	
	
	
	
	
	synchronized public void setFiles(String[] xfiles)
	{
		long[] times = makeTimes(xfiles);
		if (isFilesChanged(xfiles, times))
		{
			index = null;
			files = xfiles;
			updateTimes = times;
		}	
	}
	
	public Index makeIndex()
	{
		IIndexParser[] parsers = { new WorkbookIndexParser(),
				new WorksheetIndexParser(), new TableIndexParser(),
				new WordDocIndexParser() };
		IIndexer[] indexers = {};

		IndexRunner ixr = new IndexRunner(parsers, indexers, new DefaultIndexer());

		Index index = new Index();
		if (files == null)
			return index;
		
		for (int i = 0; i < files.length; i++)
		{
			System.out.print("Indexing " + files[i] + " ... ");
			long start = System.currentTimeMillis();

			FileSourceCodeModule source = new FileSourceCodeModule(files[i], null);

			IIndexElement src = null;
			if (files[i].endsWith(".xls"))
				src = new XlsWorkbookSourceCodeModule(source);
			else
			{
			    try
			    {
			    	src = new WordDocSourceCodeModule(source);
			    }
			    catch(Throwable t)
			    {
			    	t.printStackTrace(System.err);
			    	Log.error("This is sometimes happenning with MS Word files using Apache POI. Hopefully either they fix it or we switch to another API", t);
			    	Log.warn("Skipping " + files[i]);
			    	continue;
			    }
			}	

			ixr.index(src, index);

			long time = System.currentTimeMillis() - start;

			System.out.println(" Elapsed Time: " + time + "ms");

		}

		return index;
	}

	public void reset()
	{
		index = null;
	}

	public synchronized Index getIndex()
	{
		if (index == null)
			index = makeIndex();
		return index;
	}

	Index index;

	public String[] getLetters()
	{
		Index idx = getIndex();
		Vector v = new Vector();

		for (Iterator iter = idx.getFirstCharMap().keySet().iterator(); iter
				.hasNext();)
		{
			String s = (String) iter.next();

			if (Character.isLetter(s.charAt(0)))
			{

				v.add(s);
			}

		}

		return (String[]) v.toArray(new String[v.size()]);

	}

	public TokenBucket[] getBuckets(String charStr)
	{
		TreeMap tm = (TreeMap) getIndex().getFirstCharMap().get(charStr);

		TokenBucket[] tb = new TokenBucket[tm.size()];
		int i = 0;
		for (Iterator iter = tm.values().iterator(); iter.hasNext(); ++i)
		{
			TokenBucket element = (TokenBucket) iter.next();
			tb[i] = element;

		}
		return tb;
	}

	public String[] getIndexStrings(String charStr)
	{
		TokenBucket[] bb = getBuckets(charStr);

		String[] is = new String[bb.length];
		for (int i = 0; i < is.length; i++)
		{
			is[i] = getBucketLink(bb[i]);
		}
		return is;
	}

	static public String getBucketLink(TokenBucket tb)
	{
		return urlLink("showIndex.jsp?value="
				+ StringTool.encodeHTMLBody(tb.displayValue()), null, tb
				.displayValue()
				+ " (" + tb.size() + ")", null);
	}

	public String[][] getResultsForQuery(String query, int maxRes, IStringFilter uriFilter)
	{
		IndexQueryParser iqp = new IndexQueryParser(query);
		IndexQuery iq = iqp.parse();

		TreeMap tm = iq.execute(getIndex());

		String[] tokens = tokens(iq.getTokensInclude(), index);

		int size = Math.min(maxRes, tm.size());
		
		Vector vres = new Vector(size);

		int cnt = 0;
		for (Iterator iter = tm.values().iterator(); iter.hasNext(); )
		{
			if (cnt >= size)
				break;
			
			HitBucket hb = (HitBucket) iter.next();
			if (uriFilter != null && !uriFilter.matchString(hb.getElement().getUri()))
				continue;
			
			++cnt;
			String[] res = new String[3];
			String uri = hb.getElement().getUri();
			res[0] = uri;
			res[1] = htmlStringWithSelections(hb.getElement().getIndexedText(), tokens);
			vres.add(res);
		}
		return (String[][])vres.toArray(new String[0][]);
	}

	public static String showElementHeader(String uri)
	{
		Map map = SourceCodeURLTool.parseUrl(uri);
		
		String file = (String)map.get(XlsURLConstants.FILE);
		String sheet = (String)map.get(XlsURLConstants.SHEET);
		
		return getFileName(file) + (sheet == null ? "" : " : " + sheet);
	}
	
	
	
	static String getFileName(String path)
	{
			
			int index = path.lastIndexOf('/');
			
			return index > 0 ? path.substring(index+1) : path;
			
	}

	public String[][] getResultsForIndex(String value)
	{
		Index index = getIndex();

		TokenBucket tb = index.findTokenBucket(value);

		Vector v = new Vector();

		String[] tokens = new String[tb.getTokens().size()];
		int i = 0;
		for (Iterator iter = tb.getTokens().values().iterator(); iter.hasNext(); ++i)
		{
			String element = (String) iter.next();
			tokens[i] = element;
		}

		i = 1;
		for (Iterator iter = tb.getIndexElements().values().iterator(); iter
				.hasNext(); ++i)
		{
			HitBucket hb = (HitBucket) iter.next();
			int N = 3;

			String[] s1 = new String[N];
			String uri = hb.getElement().getUri();
			s1[0] = uri;
			s1[1] = htmlStringWithSelections(hb.getElement().getIndexedText(), tokens);
			v.add(s1);
		}

		return (String[][]) v.toArray(new String[v.size()][]);
	}

	public static String urlLink(String url, String title, String text, String target)
	{
		String s1 = "<a href=\"" + url + "\"";
		if (title != null)
			s1 += " title=\"" + title + "\"";
		if (target != null)
			s1 += " target=" + "\"" + target + "\"";
		s1 += ">" + text + "</a>";
		return s1;
	}



	static String[] tokens(String[][] src, Index idx)
	{
		List<String> v = new ArrayList<String>();

		for (int i = 0; i < src.length; i++)
		{
			for (int j = 0; j < src[i].length; j++)
			{
				String tx = src[i][j];
				TokenBucket tb = idx.findTokenBucket(tx);
				if (tb == null)
				{
					v.add(tx);
					continue;
				}	
				for (Iterator<String> iter = tb.getTokens().values().iterator(); iter.hasNext();)
				{
					v.add(iter.next());
				}

			}
		}

		String[] ret = v.toArray(new String[v.size()]);

		Arrays.sort(ret, Index.TokenBucket.TOKEN_COMPARATOR);
		return ret;
	}

	public static void main2(String[] args)
	{
		IIndexParser[] parsers = { new WorkbookIndexParser(),
				new WorksheetIndexParser(), new TableIndexParser() };
		IIndexer[] indexers = {};

		IndexRunner ixr = new IndexRunner(parsers, indexers, new DefaultIndexer());

		Index index = new Index();

		String file = "C:\\__exigen\\customer\\SRP\\ContractAttributes_5.5.14.xls";
		// "tst/xls/TestLookup.xls"
		FileSourceCodeModule source = new FileSourceCodeModule(file, null);

		XlsWorkbookSourceCodeModule src = new XlsWorkbookSourceCodeModule(source);

		ixr.index(src, index);

		// String[][] included = {{"account", "id" }};
		// String[][] excluded = {{"unique"}};
		// IIndexElement[] includedInd = {};
		//		
		// IndexQuery iq = new IndexQuery(included, excluded, includedInd);

		IndexQueryParser iqp = new IndexQueryParser("account id unique");
		IndexQuery iq = iqp.parse();

		TreeMap tm = iq.execute(index);

		String[] tokens = tokens(iq.getTokensInclude(), index);

		for (Iterator iter = tm.values().iterator(); iter.hasNext();)
		{
			HitBucket hb = (HitBucket) iter.next();
			String text = hb.getElement().getIndexedText();
			String res = htmlStringWithSelections(text, tokens);

			System.out.println("\n++++++++++++++++++++++\n" + hb.getWeight());
			System.out.println(res);
		}

	}

	public static void main(String[] args)
	{
		IIndexParser[] parsers = { new WorkbookIndexParser(),
				new WorksheetIndexParser(), new TableIndexParser() };
		IIndexer[] indexers = {};

		IndexRunner ixr = new IndexRunner(parsers, indexers, new DefaultIndexer());

		Index index = new Index();

		String file = "C:\\__exigen\\customer\\SRP\\ContractAttributes_5.5.14.xls";
		// "tst/xls/TestLookup.xls"

		for (int i = 0; i < 10; ++i)
		{
			FileSourceCodeModule source = new FileSourceCodeModule(file, null);

			XlsWorkbookSourceCodeModule src = new XlsWorkbookSourceCodeModule(source);

			ixr.index(src, index);
		}

	}

}

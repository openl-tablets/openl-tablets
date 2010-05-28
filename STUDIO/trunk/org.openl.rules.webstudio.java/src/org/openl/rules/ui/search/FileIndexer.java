package org.openl.rules.ui.search;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.openl.main.SourceCodeURLConstants;
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
import org.openl.rules.tableeditor.model.ui.util.HTMLHelper;
import org.openl.rules.word.WordDocSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.util.FileTypeHelper;
import org.openl.util.Log;

/**
 * Handles Word and Excel files for indexing. Check if files where changed in time. If true,
 * reindex it. 
 * @author snshor
 *
 */
public class FileIndexer {
    
    private long[] updateTimes;

    private String[] files = null;

    private Index index;

    static String getFileName(String path) {
        String fileName = StringUtils.EMPTY;
        if (StringUtils.isNotBlank(path)) {
            int index = path.lastIndexOf('/');

            fileName = index > 0 ? path.substring(index + 1) : path;
        }

        return fileName;
    }

    public static void main(String[] args) {
        IIndexParser[] parsers = { new WorkbookIndexParser(), new WorksheetIndexParser(), new TableIndexParser() };
        IIndexer[] indexers = {};

        IndexRunner ixr = new IndexRunner(parsers, indexers, new DefaultIndexer());

        Index index = new Index();

        String file = "C:\\__exigen\\customer\\SRP\\ContractAttributes_5.5.14.xls";
        // "tst/xls/TestLookup.xls"

        for (int i = 0; i < 10; ++i) {
            FileSourceCodeModule source = new FileSourceCodeModule(file, null);

            XlsWorkbookSourceCodeModule src = new XlsWorkbookSourceCodeModule(source);

            ixr.index(src, index);
        }

    }

    public static void main2(String[] args) {
        IIndexParser[] parsers = { new WorkbookIndexParser(), new WorksheetIndexParser(), new TableIndexParser() };
        IIndexer[] indexers = {};

        IndexRunner ixr = new IndexRunner(parsers, indexers, new DefaultIndexer());

        Index index = new Index();

        String file = "C:\\__exigen\\customer\\SRP\\ContractAttributes_5.5.14.xls";
        // "tst/xls/TestLookup.xls"
        FileSourceCodeModule source = new FileSourceCodeModule(file, null);

        XlsWorkbookSourceCodeModule src = new XlsWorkbookSourceCodeModule(source);

        ixr.index(src, index);

        IndexQuery iq = IndexQueryParser.parse("account id unique");

        TreeSet<HitBucket> hitBuckets = iq.executeSearch(index);

        String[] tokens = tokens(iq.getTokensInclude(), index);

        for (HitBucket hb : hitBuckets) {            
            String text = hb.getElement().getIndexedText();
            String res = HTMLHelper.htmlStringWithSelections(text, tokens);

            System.out.println("\n++++++++++++++++++++++\n" + hb.getWeight());
            System.out.println(res);
        }

    }
    
    /**
     * Gets the last times of file modifications.
     * 
     * @param xfiles
     * @return Times of last file modifications.
     */
    static long[] getLastModifTime(String[] xfiles) {
        long[] times = new long[xfiles.length];

        for (int i = 0; i < xfiles.length; i++) {
            times[i] = new File(xfiles[i]).lastModified();
        }
        return times;
    }

    public static String showElementHeader(String uri) {
        Map map = SourceCodeURLTool.parseUrl(uri);

        String file = (String) map.get(SourceCodeURLConstants.FILE);
        String sheet = (String) map.get(XlsURLConstants.SHEET);

        return getFileName(file) + (sheet == null ? "" : " : " + sheet);
    }

    static String[] tokens(String[][] src, Index idx) {
        List<String> v = new ArrayList<String>();

        for (int i = 0; i < src.length; i++) {
            for (int j = 0; j < src[i].length; j++) {
                String tx = src[i][j];
                TokenBucket tb = idx.findEqualsTokenBucket(tx);
                if (tb == null) {  
                    v.add(tx);
                    continue;
                }
                for (Iterator<String> iter = tb.getTokens().iterator(); iter.hasNext();) {
                    v.add(iter.next());
                }

            }
        }

        String[] ret = v.toArray(new String[v.size()]);

        Arrays.sort(ret, Index.TokenBucket.TOKEN_COMPARATOR);
        return ret;
    }

    /**
     * Gets the buckets on the specified letter
     * @param charStr Capital letter on which you want to get all buckets from index.
     * @return Array of {@link TokenBucket}
     */
    public TokenBucket[] getBuckets(String charStr) {
        TreeMap tm = getIndex().getFirstCharMap().get(charStr);

        TokenBucket[] tokenBucket = new TokenBucket[tm.size()];
        int i = 0;
        for (Iterator iter = tm.values().iterator(); iter.hasNext(); ++i) {
            tokenBucket[i] = (TokenBucket) iter.next();
        }
        return tokenBucket;
    }
    
    /**
     * Gets the index. If null index existing files.
     * @return index
     */
    public synchronized Index getIndex() {
        if (index == null) {
            index = makeIndex();
        }
        return index;
    }

    /**
     * Gets the string array of capital letters for which there are words in files.
     * @return String array of capital letters for which there are words in files. 
     */
    public String[] getLetters() {
        Index idx = getIndex();
        Vector result = new Vector();

        for (Iterator iter = idx.getFirstCharMap().keySet().iterator(); iter.hasNext();) {
            String s = (String) iter.next();
            if (Character.isLetter(s.charAt(0))) {
                result.add(s);
            }
        }
        return (String[]) result.toArray(new String[result.size()]);
    }
    
    /**
     * Gets the result from indexed files satisfying search query request.
     * 
     * @param searchQuery Query for search.
     * @return Double string array, contains the uri to element and piece of text where 
     * search query request was found.
     */
    public String[][] getResultsForIndex(String searchQuery) {        
        TokenBucket tb = getIndex().findEqualsTokenBucket(searchQuery);

        Vector v = new Vector();

        String[] tokens = new String[tb.getTokens().size()];
        int i = 0;
        for (String element : tb.getTokens()) {            
            tokens[i] = element;
            i++;
        }

        i = 1;
        for (Iterator iter = tb.getIndexElements().values().iterator(); iter.hasNext(); ++i) {
            HitBucket hb = (HitBucket) iter.next();
            String[] s1 = new String[3]; 
            s1[0] = hb.getElement().getUri();
            s1[1] = HTMLHelper.htmlStringWithSelections(hb.getElement().getIndexedText(), tokens);
            v.add(s1);
        }

        return (String[][]) v.toArray(new String[v.size()][]);
    }
    
    /**
     * Gets the result from indexed files satisfying search query request and filtered 
     * with uri filter.
     * 
     * @param searchQuery Query for search.
     * @param maxRes Max number of results satisfying the search criteria.
     * @param uriFilter Filter for results. If null, all results will be taken.
     * @return Double string array, contains the uri to element and piece of text where 
     * search query request was found.
     */
    public String[][] getResultsForQuery(String searchQuery, int maxRes, IStringFilter uriFilter) {        
        IndexQuery indexQuery = IndexQueryParser.parse(searchQuery);
                
        TreeSet<HitBucket> searchRes = indexQuery.executeSearch(getIndex());

        String[] tokens = tokens(indexQuery.getTokensInclude(), index);

        int size = Math.min(maxRes, searchRes.size());

        Vector result = new Vector(size);

        int cnt = 0;
        for (HitBucket hb : searchRes) {
            if (cnt >= size) {
                break;
            }
            
            if (uriFilter != null && !uriFilter.matchString(hb.getElement().getUri())) {
                continue;
            }            
            String[] res = new String[3];
            String uri = hb.getElement().getUri();
            res[0] = uri;
            res[1] = HTMLHelper.htmlStringWithSelections(hb.getElement().getIndexedText(), tokens);
            result.add(res);
            ++cnt;
        }
        return (String[][]) result.toArray(new String[0][]);
    }
    
    /**
     * Check whether the files were changed.
     * @param xfiles
     * @param times
     * @return
     */
    public boolean isFilesChanged(String[] xfiles, long[] times) {
        boolean result = false;
        
        if(files == null || updateTimes == null || files.length != xfiles.length) {
            result = true;
        } else {
            for (int i = 0; i < xfiles.length; i++) {
                if (!xfiles[i].equals(files[i])) {
                    result = true;
                }
            }
            for (int i = 0; i < times.length; i++) {
                if (times[i] != updateTimes[i]) {
                    result = true;
                }
            }
        }        
        return result;
    }
    
    /**
     * Index all the files in project according to their format (Word or Excel). 
     * @return Index for files.
     */
    public Index makeIndex() {
        Index indexResult = new Index();
        if (files != null) {
            IIndexParser[] parsers = { new WorkbookIndexParser(), new WorksheetIndexParser(), new TableIndexParser(),
                    new WordDocIndexParser() };
            IIndexer[] indexers = {};

            IndexRunner indexRunner = new IndexRunner(parsers, indexers, new DefaultIndexer());            

            for (String file : files) {
                System.out.print("Indexing " + file + " ... ");
                long start = System.currentTimeMillis();

                FileSourceCodeModule source = new FileSourceCodeModule(file, null);

                IIndexElement srcToIndex = null;
                if (FileTypeHelper.isExcelFile(file)) {
                    srcToIndex = new XlsWorkbookSourceCodeModule(source);
                } else {
                    try {
                        srcToIndex = new WordDocSourceCodeModule(source);
                    } catch (Throwable t) {
                        t.printStackTrace(System.err);
                        Log.error("This is sometimes happenning with MS Word files using Apache POI. " +
                        		"Hopefully either they fix it or we switch to another API", t);
                        Log.warn("Skipping " + file);
                        continue;
                    }
                }

                indexRunner.index(srcToIndex, indexResult);

                long time = System.currentTimeMillis() - start;

                System.out.println(" Elapsed Time: " + time + "ms");
            }
        }         
        return indexResult;
    }
    
    /**
     * Reset index to null. To reinitialize it call getIndex().
     */
    public void reset() {
        index = null;
    }
    
    /**
     * Sets the files for further indexing.
     * @param xfiles
     */
    synchronized public void setFiles(String[] xfiles) {
        long[] times = getLastModifTime(xfiles);
        if (isFilesChanged(xfiles, times)) {
            index = null;
            files = xfiles;
            updateTimes = times;
        }
    }

}

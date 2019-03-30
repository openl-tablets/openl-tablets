/*
 * Created on Dec 23, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.rules.table.xls;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.openl.rules.table.syntax.XlsURLConstants;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;

public class XlsUrlParser implements XlsURLConstants {

    private String wbPath;
    private String wbName;
    private String wsName;

    private String range;
    private String cell;

    public XlsUrlParser() { /* NON */ }

    public XlsUrlParser(String url) {
        parse(url);
    }
    
    public void parse(String url) {
        String file;
        Map<String, String> map = new HashMap<>();
        int indexQuestionMark = url.indexOf('?');
        if (indexQuestionMark >= 0) {
            file = url.substring(0, indexQuestionMark);
            String query = url.substring(indexQuestionMark + 1);

            StringTokenizer st = new StringTokenizer(query, QSEP);

            while (st.hasMoreTokens()) {
                String pair = st.nextToken();

                int idx = pair.indexOf('=');

                if (idx < 0) {
                    map.put(pair, "");
                } else {
                    String key = pair.substring(0, idx);
                    String value = StringTool.decodeURL(pair.substring(idx + 1, pair.length()));
                    map.put(key, value);
                }
            }
        } else {
            file = url;
        }
        file = StringTool.decodeURL(file);
        wsName = map.get(SHEET);
        range = map.get(RANGE);
        cell = map.get(CELL);

        if (range == null) {
            // TODO line, col
            range = cell;
        }
        
        if (cell == null && range != null) {
            cell = range.substring(0, range.indexOf(":"));
        }

        if ("null".equals(file)) {
            // there is no file representation
            // FIXME tempory hack to support generated dispatch tables
            wbPath = "/unexistingPath/";
            wbName = "unexistingSourceFile.xls";
        } else {
            if (file.startsWith("file:/")) {
                // In current OpenL implementation in Linux the path will be like this: file:/opt/smth.
                // In Windows like this: file:/C:/smth.
                int prefixSize = file.length() > 7 && file.charAt(7) == ':' ? 6 : 5;
                file = file.substring(prefixSize);
            }
            try {
                File f = new File(file).getCanonicalFile();
                wbPath = f.getParent();
                wbName = f.getName();
            } catch (IOException e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }
        }
    }

    public String getWbPath() {
        return wbPath;
    }

    public String getWbName() {
        return wbName;
    }

    public String getWsName() {
        return wsName;
    }

    public String getRange() {
        return range;
    }

    public String getCell() {
        return cell;
    }
    
    
}

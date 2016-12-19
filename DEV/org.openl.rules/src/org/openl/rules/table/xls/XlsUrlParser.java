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

/**
 * @author sam
 */

public class XlsUrlParser implements XlsURLConstants {

    public String wbPath;
    public String wbName;
    public String wsName;

    public String range;
    public String cell;

    public void parse(String url) {

        String file;
        Map<String, String> map = new HashMap<String, String>();
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

        if ("null".equals(file)) {
            // there is no file representation
            // FIXME tempory hack to support generated dispatch tables
            wbPath = "/unexistingPath/";
            wbName = "unexistingSourceFile.xls";
        } else {
            if (file.startsWith("file:/")) {
                file = file.substring(6);
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

}

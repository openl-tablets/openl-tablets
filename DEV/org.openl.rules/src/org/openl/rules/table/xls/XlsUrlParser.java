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

import org.openl.rules.table.IGridRegion;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;

public class XlsUrlParser {

    private final String wbPath;
    private final String wbName;
    private final String wsName;

    private final String range;
    private final String cell;

    public XlsUrlParser(String url) {
        String file;
        Map<String, String> map = new HashMap<>();
        int indexQuestionMark = url.indexOf('?');
        if (indexQuestionMark >= 0) {
            file = url.substring(0, indexQuestionMark);
            String query = url.substring(indexQuestionMark + 1);

            StringTokenizer st = new StringTokenizer(query, "&");

            while (st.hasMoreTokens()) {
                String pair = st.nextToken();

                int idx = pair.indexOf('=');

                if (idx < 0) {
                    map.put(pair, "");
                } else {
                    String key = pair.substring(0, idx);
                    String value = pair.substring(idx + 1);
                    if ("sheet".equals(key)) {
                        value = StringTool.decodeURL(value);
                    }
                    map.put(key, value);
                }
            }
        } else {
            file = url;
        }
        file = StringTool.decodeURL(file);
        wsName = map.get("sheet");
        String range = map.get("range");
        String cell = map.get("cell");

        if (range == null) {
            // TODO line, col
            range = cell;
        }

        if (cell == null && range != null) {
            cell = range.substring(0, range.indexOf(":"));
        }

        this.range = range;
        this.cell = cell;

        if ("null".equals(file)) {
            // there is no file representation
            // FIXME temporary hack to support generated dispatch tables
            wbPath = "/unexistingPath/";
            wbName = "unexistingSourceFile.xls";
        } else {
            if (file != null && file.startsWith("file:/")) {
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

    public boolean intersects(XlsUrlParser p2) {
        if (!wbPath.equals(p2.wbPath) || !wbName.equals(p2.wbName) || !wsName.equals(p2.wsName)) {
            return false;
        }

        if (range == null || p2.range == null) {
            return false;
        }

        IGridRegion i1 = IGridRegion.Tool.makeRegion(range);
        return IGridRegion.Tool.intersects(i1, IGridRegion.Tool.makeRegion(p2.range));
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

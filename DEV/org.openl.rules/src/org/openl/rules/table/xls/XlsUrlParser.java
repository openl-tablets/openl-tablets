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

import lombok.Getter;

import org.openl.rules.table.GridRegionUtils;
import org.openl.rules.table.IGridRegion;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;

public class XlsUrlParser {

    private static final String VIRTUAL_GRID_FOLDER = "/unexistingPath/";

    @Getter
    private final String wbPath;
    @Getter
    private final String wbName;
    @Getter
    private final String wsName;

    @Getter
    private final String range;
    @Getter
    private final String cell;

    public XlsUrlParser(String url) {
        String file;
        var map = new HashMap<String, String>();
        var indexQuestionMark = url.indexOf('?');
        if (indexQuestionMark >= 0) {
            file = url.substring(0, indexQuestionMark);
            var query = url.substring(indexQuestionMark + 1);

            parseQuery(query, map);
        } else {
            file = url;
        }
        file = StringTool.decodeURL(file);
        wsName = map.get("sheet");
        var rangeRef = map.get("range");
        var cellRef = map.get("cell");

        if (rangeRef == null) {
            rangeRef = cellRef;
        }

        if (cellRef == null && rangeRef != null) {
            cellRef = rangeRef.substring(0, rangeRef.indexOf(":"));
        }

        this.range = rangeRef;
        this.cell = cellRef;

        if ("null".equals(file)) {
            // there is no file representation
            // FIXME temporary hack to support generated dispatch tables
            wbPath = VIRTUAL_GRID_FOLDER;
            wbName = "unexistingSourceFile.xls";
        } else {
            var f = toCanonicalFile(file);
            wbPath = f.getParent();
            wbName = f.getName();
        }
    }

    private static void parseQuery(String query, Map<String, String> map) {
        var st = new StringTokenizer(query, "&");

        while (st.hasMoreTokens()) {
            var pair = st.nextToken();

            var idx = pair.indexOf('=');

            if (idx < 0) {
                map.put(pair, "");
            } else {
                var key = pair.substring(0, idx);
                var value = pair.substring(idx + 1);
                if ("sheet".equals(key)) {
                    value = StringTool.decodeURL(value);
                }
                map.put(key, value);
            }
        }
    }

    private static File toCanonicalFile(String file) {
        if (file != null && file.startsWith("file:/")) {
            // In current OpenL implementation in Linux the path will be like this: file:/opt/smth.
            // In Windows like this: file:/C:/smth.
            int prefixSize = file.length() > 7 && file.charAt(7) == ':' ? 6 : 5;
            file = file.substring(prefixSize);
        }
        try {
            return new File(file).getCanonicalFile();
        } catch (IOException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    public boolean intersects(XlsUrlParser p2) {
        if (!wbPath.equals(p2.wbPath) || !wbName.equals(p2.wbName) || !wsName.equals(p2.wsName)) {
            return false;
        }

        if (range == null || p2.range == null) {
            return false;
        }

        IGridRegion i1 = GridRegionUtils.makeRegion(range);
        return GridRegionUtils.intersects(i1, GridRegionUtils.makeRegion(p2.range));
    }

}

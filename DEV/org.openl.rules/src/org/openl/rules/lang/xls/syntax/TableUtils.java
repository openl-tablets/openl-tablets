package org.openl.rules.lang.xls.syntax;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.openl.util.StringTool;

/**
 * Created by Andrei Ostrovski on 14.06.14.
 */
public final class TableUtils {

    private TableUtils() {
    }

    public static String makeTableId(String uri) {
        return Optional.ofNullable(uri)
            .map(StringTool::decodeURL)
            .map(TableUtils::toCellURI)
            .map(DigestUtils::md5Hex)
            .orElse(null);
    }

    /**
     * Transforms range query parameter to cell
     *
     * <pre>
     *     file:///foo.xlsx?sheet=Sheet1&range=A1:A22      file:///foo.xlsx?sheet=Sheet1&cell=A1
     *     file:///foo.xlsx?sheet=Sheet1&cell=A1           file:///foo.xlsx?sheet=Sheet1&cell=A1
     * </pre>
     *
     * @param rawUri old URI
     * @return new URI, othervice old
     */
    static String toCellURI(String rawUri) {
        final int querySep = rawUri.indexOf('?');
        if (querySep > 0) {
            final String rawQuery = rawUri.substring(querySep + 1);
            Map<String, String> query = new LinkedHashMap<>();
            for (String pair : rawQuery.split("&")) {
                int idx = pair.indexOf('=');
                if (idx < 0) {
                    query.put(pair, null);
                } else {
                    query.put(pair.substring(0, idx), pair.substring(idx + 1));
                }
            }
            if (query.containsKey("range")) {
                final StringBuilder newUri = new StringBuilder();
                newUri.append(rawUri, 0, querySep).append('?');
                int idx = 0;
                for (Map.Entry<String, String> pair : query.entrySet()) {
                    if (idx > 0) {
                        newUri.append('&');
                    }
                    if ("range".equals(pair.getKey())) {
                        newUri.append("cell").append('=');
                        String value = pair.getValue();
                        newUri.append(value, 0, value.indexOf(':'));
                    } else {
                        newUri.append(pair.getKey()).append('=').append(pair.getValue());
                    }
                    idx++;
                }
                return newUri.toString();
            }
        }
        return rawUri;
    }

}

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
        final var querySep = rawUri.indexOf('?');
        if (querySep > 0) {
            final var rawQuery = rawUri.substring(querySep + 1);
            var query = new LinkedHashMap<String, String>();
            for (String pair : rawQuery.split("&")) {
                var idx = pair.indexOf('=');
                if (idx < 0) {
                    query.put(pair, null);
                } else {
                    query.put(pair.substring(0, idx), pair.substring(idx + 1));
                }
            }
            if (query.containsKey("range")) {
                final var newUri = new StringBuilder();
                newUri.append(rawUri, 0, querySep).append('?');
                var idx = 0;
                for (Map.Entry<String, String> pair : query.entrySet()) {
                    if (idx > 0) {
                        newUri.append('&');
                    }
                    if ("range".equals(pair.getKey())) {
                        newUri.append("cell").append('=');
                        var value = pair.getValue();
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

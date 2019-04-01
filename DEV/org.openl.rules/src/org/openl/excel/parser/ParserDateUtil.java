package org.openl.excel.parser;

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.DateUtil;

public final class ParserDateUtil {

    private static class CacheKey {
        int formatIndex;
        String formatString;

        public CacheKey(int formatIndex, String formatString) {
            super();
            this.formatIndex = formatIndex;
            this.formatString = formatString;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + formatIndex;
            result = prime * result + ((formatString == null) ? 0 : formatString.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            CacheKey other = (CacheKey) obj;
            if (formatIndex != other.formatIndex) {
                return false;
            }
            if (formatString == null) {
                if (other.formatString != null) {
                    return false;
                }
            } else if (!formatString.equals(other.formatString)) {
                return false;
            }
            return true;
        }
    }

    private Map<CacheKey, Boolean> cache = new HashMap<>();

    public boolean isADateFormat(int formatIndex, String formatString) {
        CacheKey key = new CacheKey(formatIndex, formatString);
        return cache.computeIfAbsent(key, e -> DateUtil.isADateFormat(formatIndex, formatString));
    }

    public void reset() {
        cache.clear();
    }
}

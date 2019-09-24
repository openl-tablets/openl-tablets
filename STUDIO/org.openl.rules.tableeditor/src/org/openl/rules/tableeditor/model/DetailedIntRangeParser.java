package org.openl.rules.tableeditor.model;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.helpers.BaseRangeParser;
import org.openl.rules.helpers.IntRangeParser;
import org.openl.rules.helpers.RangeParser;
import org.openl.util.RangeWithBounds;

public class DetailedIntRangeParser extends IntRangeParser {
    private String min;
    private String max;

    @Override
    public RangeWithBounds parse(String range) {
        try {
            range = range.trim();
            for (RangeParser parser : PARSERS) {
                RangeWithBounds value;

                value = parser.parse(range);
                if (value != null) {
                    if (parser instanceof BaseRangeParser) {
                        BaseRangeParser baseRangeParser = (BaseRangeParser) parser;
                        min = baseRangeParser.getMinNumber() + baseRangeParser.getMinMultiplier();
                        max = baseRangeParser.getMaxNumber() + baseRangeParser.getMaxMultiplier();
                    }
                    return value;
                }
            }
        } catch (RuntimeException e) {
            throw new OpenLRuntimeException("Failed to parse double range.", e);
        }
        throw new OpenLRuntimeException("Failed to parse double range.");
    }

    public String getMin() {
        return min;
    }

    public String getMax() {
        return max;
    }
}

package org.openl.rules.tableeditor.model;

import org.openl.rules.helpers.BaseRangeParser;
import org.openl.rules.helpers.IntRangeParser;
import org.openl.rules.helpers.RangeParser;
import org.openl.util.RangeWithBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            // Shouldn't occur. But if occurs, log exception and fallback to grammar parser
            Logger log = LoggerFactory.getLogger(RangeWithBounds.class);
            log.error(e.getMessage(), e);
        }

        RangeWithBounds parse = FALLBACK_PARSER.parse(range);
        min = String.valueOf(parse.getMin());
        max = String.valueOf(parse.getMax());
        return parse;
    }

    public String getMin() {
        return min;
    }

    public String getMax() {
        return max;
    }
}

package org.openl.rules.helpers;

import org.openl.OpenL;
import org.openl.engine.OpenLManager;
import org.openl.source.SourceType;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.util.RangeWithBounds;

final public class BExGrammarParser implements RangeParser {
    private final SourceType sourceType;

    public BExGrammarParser(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    private OpenL openl;

    public OpenL getOpenL() {
        if (openl == null) {
            openl = OpenL.getInstance(OpenL.OPENL_J_NAME);
        }
        return openl;
    }

    @Override
    public RangeWithBounds parse(String range) {
        // TODO: Correct tokenizing in grammar.
        return (RangeWithBounds) OpenLManager.run(getOpenL(), new StringSourceCodeModule(range, ""), sourceType);
    }
}

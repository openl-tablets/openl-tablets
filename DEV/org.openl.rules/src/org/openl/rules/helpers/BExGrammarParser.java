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

    @Override
    public RangeWithBounds parse(String range) {
        // TODO: Correct tokenizing in grammar.
        OpenL openl = OpenL.getInstance(OpenL.OPENL_J_NAME);
        return (RangeWithBounds) OpenLManager.run(openl, new StringSourceCodeModule(range, ""), sourceType);
    }
}

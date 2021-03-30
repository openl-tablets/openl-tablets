package org.openl.rules.calc.result;

import org.openl.rules.calc.SpreadsheetResultCalculator;

/**
 * Performs {@link org.openl.rules.calc.Spreadsheet} cells calculation and returns null. Must be used when return type
 * is {@code void} or {@code java.lang.Void}
 *
 * @author Vladyslav Pikus
 */
public class EmptyResultBuilder implements IResultBuilder {

    @Override
    public Object buildResult(SpreadsheetResultCalculator result) {
        result.getValues();
        return null;
    }
}

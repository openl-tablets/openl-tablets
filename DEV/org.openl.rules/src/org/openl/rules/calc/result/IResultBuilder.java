package org.openl.rules.calc.result;

import org.openl.rules.calc.SpreadsheetResultCalculator;

public interface IResultBuilder {

    Object makeResult(SpreadsheetResultCalculator res);

}

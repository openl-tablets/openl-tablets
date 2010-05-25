package org.openl.rules.calc.result;

import org.openl.rules.calc.SpreadsheetResult;


public interface IResultBuilder {

    Object makeResult(SpreadsheetResult res);

}

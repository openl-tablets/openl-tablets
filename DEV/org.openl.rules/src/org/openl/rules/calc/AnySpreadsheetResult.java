package org.openl.rules.calc;

import javax.xml.bind.annotation.XmlRootElement;

import org.openl.types.java.CustomJavaOpenClass;

@XmlRootElement
@CustomJavaOpenClass(type = AnySpreadsheetResultOpenClass.class, variableInContextFinder = SpreadsheetResultRootDictionaryContext.class)
public class AnySpreadsheetResult extends SpreadsheetResult {
    public AnySpreadsheetResult() {
    }
}

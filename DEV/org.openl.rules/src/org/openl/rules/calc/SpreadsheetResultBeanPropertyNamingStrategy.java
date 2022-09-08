package org.openl.rules.calc;

/**
 * This is interface is designed to control converting spreadsheet result column/row names to map keys.
 */
public interface SpreadsheetResultBeanPropertyNamingStrategy {

    String transform(String name);

    String transform(String column, String row);
}

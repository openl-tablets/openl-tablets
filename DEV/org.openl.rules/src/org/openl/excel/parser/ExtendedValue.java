package org.openl.excel.parser;

/**
 * Interface to mark a class that an object has a special meaning and need to deal with specific instance when use
 * specific cell value.
 */
public interface ExtendedValue {
    Object getValue();
}

package org.openl.rules.helpers;

public abstract class ARangeParser<T> {

    ARangeParser() {
    }

    public static final class ParseStruct<T> {

        public enum BoundType {
            INCLUDING,
            EXCLUDING
        }
    }
}

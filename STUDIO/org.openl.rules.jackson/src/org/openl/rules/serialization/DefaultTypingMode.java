package org.openl.rules.serialization;

public enum DefaultTypingMode {
    JAVA_LANG_OBJECT,
    OBJECT_AND_NON_CONCRETE,
    NON_CONCRETE_AND_ARRAYS,
    NON_FINAL,
    NON_FINAL_AND_ENUMS,
    /**
     * @deprecated in Jackson
     */
    EVERYTHING,
    DISABLED
}

package org.openl.rules.testmethod;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author snshor
 *
 */
public class TestMethodHelper {
    public static final String ROW_ID = "_id_";

    /** Field name for the expected result in test */
    public static final String EXPECTED_RESULT_NAME = "_res_";

    /** Field name for the expecting eror in test */
    public static final String EXPECTED_ERROR = "_error_";

    /** Field name for defining runtime context in test */
    public static final String CONTEXT_NAME = "_context_";

    /** Field name for test unit description in test */
    public static final String DESCRIPTION_NAME = "_description_";

    public static final Set<String> RESERVED_COLUMN_NAMES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(ROW_ID, EXPECTED_ERROR, EXPECTED_RESULT_NAME, CONTEXT_NAME, DESCRIPTION_NAME)));

}

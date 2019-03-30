package org.openl.rules.cmatch.matcher;

import org.openl.types.IOpenClass;

public interface IMatcherBuilder {

    /**
     * Both are equal or actual value is in range.
     */
    String OP_MATCH = "match";

    /**
     * variable is bigger or equal to check value
     */
    String OP_MIN = "min";

    /**
     * variable is less or equal to check value
     */
    String OP_MAX = "max";

    /**
     * Get instance of matcher for a given type.
     * <p>
     * It can create new instance of the matcher each time or return *itself* if one matcher is enough. It is useful for
     * enums and e.t.c
     *
     * @param type variable's class
     * @return null if {@literal type} is not supported; or instance of a matcher
     */
    IMatcher getInstanceIfSupports(IOpenClass type);

    /**
     * Get name of match operation.
     *
     * @return name of operation
     */
    String getName();
}

package org.openl.engine;

import org.openl.OpenL;

/**
 * Class that defines an abstraction of OpenL engine manager.
 */
public abstract class BaseOpenlManager {

    /**
     * {@link OpenL} instance. Actually OpenL engine context that used during
     * core operations such as parsing, binding, processing, compilation and
     * etc.
     */
    private OpenL openl;

    /**
     * Base constructor.
     * 
     * @param openl {@link OpenL} instance
     */
    public BaseOpenlManager(OpenL openl) {
        this.openl = openl;
    }

    /**
     * Gets OpenL engine context instance.
     * 
     * @return
     */
    protected OpenL getOpenL() {
        return openl;
    }

}

/*
 * Created on Nov 14, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.util.Collection;

/**
 * @author snshor
 *
 */
public class TopoSortCycleException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = -6748402650936894385L;
    Collection<?> cycles;

    public TopoSortCycleException(Collection<?> cycles) {
        this.cycles = cycles;
    }

    /**
     * @return
     */
    public Collection<?> getCycles() {
        return cycles;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Throwable#getMessage()
     */
    @Override
    public String getMessage() {

        return "TopoSort Cycle Error:" + cycles;
    }

}

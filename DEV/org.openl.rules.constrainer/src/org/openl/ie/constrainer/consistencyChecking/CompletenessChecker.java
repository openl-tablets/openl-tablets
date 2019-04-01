package org.openl.ie.constrainer.consistencyChecking;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import java.util.List;

public interface CompletenessChecker {
    /**
     * returns Vector of uncovered regions in the space of states
     */

    List<Uncovered> check();
}
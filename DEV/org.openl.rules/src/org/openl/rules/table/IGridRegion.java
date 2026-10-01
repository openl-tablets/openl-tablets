/*
 * Created on Oct 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

/**
 * @author snshor
 */
public interface IGridRegion {
    IGridRegion[] EMPTY_REGION = new IGridRegion[0];

    int getBottom();

    int getLeft();

    int getRight();

    int getTop();

}

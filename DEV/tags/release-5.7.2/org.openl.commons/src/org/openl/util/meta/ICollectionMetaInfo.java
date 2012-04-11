/*
 * Created on May 5, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.util.meta;

import org.openl.domain.IType;

/**
 * @author snshor
 */
public interface ICollectionMetaInfo {
    // boolean isReadOnly();
    // boolean isAddOnly();

    double getAddReadRatio();

    IType getComponentType();

    int getInitialSize();

    int getMaxSize();

    IOrderMetaInfo getOrderedMetaInfo();

    double getRemoveReadRatio();
}

package org.openl.rules.repository;

import java.util.Date;

/**
 * OpenL Rules Entity Version.
 *
 * @author Aleh Bykhavets
 *
 */
public interface RVersion extends CommonVersion {

    /**
     * Gets date when the version was created.
     *
     * @return date of creation
     */
    Date getCreated();

    /**
     * Gets user who created the version.
     *
     * @return user who created it
     */
    CommonUser getCreatedBy();

}

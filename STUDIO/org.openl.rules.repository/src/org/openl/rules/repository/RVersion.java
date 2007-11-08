package org.openl.rules.repository;

import java.util.Date;

/**
 * OpenL Rules Entity Version.
 *
 * @author Aleh Bykhavets
 *
 */
public interface RVersion {
    /**
     * Gets version's name.
     *
     * @return name of version
     */
    public String getName();

    /**
     * Gets date when the version was created.
     *
     * @return date of creation
     */
    public Date getCreated();

    /**
     * Gets user who created the version.
     *
     * @return user who created it
     */
    public RUser getCreatedBy();
    
    public int getMajor();
    public int getMinor();
    public int getRevision();
}

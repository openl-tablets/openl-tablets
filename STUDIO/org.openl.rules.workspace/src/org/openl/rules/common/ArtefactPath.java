package org.openl.rules.common;

import java.util.Collection;

/**
 * Defines path of an Artefact. I.e. location of some particular artefact in a hierarchy of artefacts or in a tree of
 * artefacts.
 *
 * @author Aleh Bykhavets
 */
public interface ArtefactPath {

    /**
     * Gets the path as a collection of segments.
     *
     * @return collection of segments
     */
    Collection<String> getSegments();

    /**
     * Gets the path as a single string. All segments are concatenated by special delimiter '/'.
     *
     * @return string with the path
     */
    String getStringValue();

    /**
     * Gets the path as a single string, omitting first <code>skip</code> elements. All segments are concatenated by
     * special delimiter '/'.
     *
     * @param skip number of elements to skip
     * @return string with the path
     */
    String getStringValue(int skip);

    /**
     * Gets a segment in the path.
     *
     * @param index position of segment in the path
     * @return value of segment
     */
    String segment(int index);

    /**
     * Returns number of segments in the path
     *
     * @return integer number of segments
     */
    int segmentCount();

}

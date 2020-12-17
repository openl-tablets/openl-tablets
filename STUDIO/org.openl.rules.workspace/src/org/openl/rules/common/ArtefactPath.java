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
     *
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

    /**
     * Create new instance of ArtefactPath from base one, excluding first segment of base path.
     * <p/>
     * It is used to translate path of artefact in the workspace to path in a project.
     *
     * @return new instance where first segment of base path is excluded.
     */
    ArtefactPath withoutFirstSegment();

    ArtefactPath withoutSegment(int segmentIndex);

    /**
     * Creates new instance of ArtefactPath from base one, adding one more segment.
     * <p/>
     * It should be used to build artefact paths recursively.
     *
     * @param segment adding segment
     * @return new instance where specified segment is appended to path
     */
    ArtefactPath withSegment(String segment);

}

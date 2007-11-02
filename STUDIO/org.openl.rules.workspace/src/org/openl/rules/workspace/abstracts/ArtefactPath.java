package org.openl.rules.workspace.abstracts;

import java.util.Collection;

/**
 * Defines path of an Artefact.
 * I.e. location of some particular artefact in a hierarchy of artefacts
 * or in a tree of artefacts.
 */
public interface ArtefactPath {
    /**
     * Gets a segment in the path.
     *
     * @param index position of segment in the path
     * @return value of segment
     */
    public String segment(int index);

    /**
     * Returns number of segments in the path
     * @return
     */
    public int segmentCount();

    /**
     * Gets the path as a collection of segments.
     *
     * @return collection of segments
     */
    public Collection<String> getSegments();

    /**
     * Gets the path as a single string.
     * All segments are concatenated by special delimiter '/'.
     *
     * @return string with the path
     */
    public String getStringValue();

    public ArtefactPath getRelativePath(int startSegment);

    public ArtefactPath add(String segment);
}

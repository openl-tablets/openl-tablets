package org.openl.rules.commons.artefacts.impl;

import org.openl.rules.commons.artefacts.ArtefactPath;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of Artefact Path.
 * <p>
 * Only absolute paths are supported now.
 */
public class ArtefactPathImpl implements ArtefactPath {
    public static final char SEGMENT_DELIMITER = '/';
//    private static final String SEGMENT_DELIMITER_STRING = "" + SEGMENT_DELIMITER;
    private String stringValue = null;
    private List<String> segments = new LinkedList<String>();

    public ArtefactPathImpl(String segments[]) {
        for (String element : segments) {
            addSegment(element);
        }
    }

    public ArtefactPathImpl(List<String> segments) {
        for (String element : segments) {
            addSegment(element);
        }
    }

    public ArtefactPathImpl(String pathAsString) {
        if ((pathAsString.length() > 0) && (pathAsString.charAt(0) == SEGMENT_DELIMITER)) {
//            this.state = State.ABSOLUTE;
            appendToSegments(pathAsString.substring(1));
        } else {
//            this.state = State.RELATIVE;
            appendToSegments(pathAsString);
        }
    }

    public ArtefactPathImpl(ArtefactPath artefactPath) {
//        this.state = artefactPath.state;
        segments.addAll(artefactPath.getSegments());
    }

    public ArtefactPath getRelativePath(int startSegment) {
        LinkedList<String> relativeSegments = new LinkedList<String>();
        int i = 0;
        for (String s : segments) {
            if (startSegment >= i) {
                relativeSegments.add(s);
            }

            i++;
        }
        
        return new ArtefactPathImpl(relativeSegments);
    }

    public ArtefactPath add(String segment) {
        ArtefactPathImpl api = new ArtefactPathImpl(this);
        api.addSegment(segment);

        return api;
    }

    /** {@inheritDoc} */
    public String segment(int index) {
        return segments.get(index);
    }

    /** {@inheritDoc} */
    public int segmentCount() {
        return segments.size();
    }

    /** {@inheritDoc} */
    public Collection<String> getSegments() {
        return segments;
    }

    protected void addSegment(String segment) {
        if (segment.indexOf(SEGMENT_DELIMITER) >= 0) {
            // TODO: error -- segment must not contain delimiter(s)
        }

        segments.add(segment);
    }

    protected void appendToSegments(String pathAsString) {
        int len = pathAsString.length();
        int pos = 0;
        for (int end = 0; end < len; ) {
            end = pathAsString.indexOf(SEGMENT_DELIMITER, pos);
            if (end < 0) end = len;

            String s = pathAsString.substring(pos, end);
            addSegment(s);

            pos = end + 1;
        }
    }

    /** {@inheritDoc} */
    public String getStringValue() {
        if (stringValue == null) {
            StringBuffer result = new StringBuffer();

//            if (state == State.ABSOLUTE) {
                result.append(SEGMENT_DELIMITER);
//            }

            for (Iterator<String> i = segments.iterator(); i.hasNext();) {
                String segment = i.next();
                result.append(segment);

                if (i.hasNext()) {
                    result.append(SEGMENT_DELIMITER);
                }
            }
            stringValue = result.toString();
        }

        return stringValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof ArtefactPathImpl) {
            ArtefactPathImpl other = (ArtefactPathImpl) obj;
            return getStringValue().equals(other.getStringValue());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getStringValue().hashCode();
    }

    @Override
    protected Object clone() {
        return new ArtefactPathImpl(this);
    }
}

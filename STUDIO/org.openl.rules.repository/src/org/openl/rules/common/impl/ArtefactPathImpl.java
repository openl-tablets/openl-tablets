package org.openl.rules.common.impl;

import org.openl.rules.common.ArtefactPath;

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
    private String stringValue = null;
    private List<String> segments = new LinkedList<String>();

    public ArtefactPathImpl(ArtefactPath artefactPath) {
        segments.addAll(artefactPath.getSegments());
    }

    public ArtefactPathImpl(List<String> segments) {
        for (String element : segments) {
            addSegment(element);
        }
    }

    public ArtefactPathImpl(String segments[]) {
        for (String element : segments) {
            addSegment(element);
        }
    }

    public ArtefactPathImpl(String pathAsString) {
        if ((pathAsString.length() > 0) && (pathAsString.charAt(0) == SEGMENT_DELIMITER)) {
            appendToSegments(pathAsString.substring(1));
        } else {
            appendToSegments(pathAsString);
        }
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
        for (int end = 0; end < len;) {
            end = pathAsString.indexOf(SEGMENT_DELIMITER, pos);
            if (end < 0) {
                end = len;
            }

            String s = pathAsString.substring(pos, end);
            addSegment(s);

            pos = end + 1;
        }
    }

    @Override
    protected Object clone() {
        return new ArtefactPathImpl(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ArtefactPathImpl) {
            ArtefactPathImpl other = (ArtefactPathImpl) obj;
            if (segmentCount() != other.segmentCount()) {
                return false;
            }
            Iterator<String> it1 = segments.iterator();
            Iterator<String> it2 = other.segments.iterator();
            while (it1.hasNext()) {
                if (!it1.next().equals(it2.next())) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> getSegments() {
        return segments;
    }

    /** {@inheritDoc} */
    @Override
    public String getStringValue() {
        if (stringValue == null) {
            StringBuilder result = new StringBuilder();

            result.append(SEGMENT_DELIMITER);

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
    public String getStringValue(int skip) {
        StringBuilder result = new StringBuilder();
        if (skip < segmentCount()) {
            Iterator<String> it = segments.iterator();
            for (; skip > 0; --skip) {
                it.next();
            }
            while (it.hasNext()) {
                result.append(SEGMENT_DELIMITER).append(it.next());
            }
        }
        return result.toString();
    }

    @Override
    public int hashCode() {
        return getStringValue().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public String segment(int index) {
        return segments.get(index);
    }

    /** {@inheritDoc} */
    @Override
    public int segmentCount() {
        return segments.size();
    }

    @Override
    public ArtefactPath withoutFirstSegment() {
        LinkedList<String> relativeSegments = new LinkedList<>();
        boolean isFisrt = true;
        for (String s : segments) {
            if (isFisrt) {
                isFisrt = false;
                continue;
            }

            relativeSegments.add(s);
        }

        return new ArtefactPathImpl(relativeSegments);
    }

    @Override
    public ArtefactPath withoutSegment(int segmentIndex) {
        LinkedList<String> relativeSegments = new LinkedList<>();
        Iterator<String> segmentIterator = segments.iterator();
        for (int i = 0; segmentIterator.hasNext(); i++) {
            if (i != segmentIndex) {
                relativeSegments.add(segmentIterator.next());
            } else {
                segmentIterator.next();
            }
        }

        return new ArtefactPathImpl(relativeSegments);
    }

    @Override
    public ArtefactPath withSegment(String segment) {
        ArtefactPathImpl api = new ArtefactPathImpl(this);
        api.addSegment(segment);

        return api;
    }
}

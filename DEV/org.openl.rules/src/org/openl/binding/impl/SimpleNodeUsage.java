package org.openl.binding.impl;

import java.util.Objects;

import org.openl.syntax.impl.IdentifierNode;
import org.openl.util.text.TextInfo;

/**
 * @author nsamatov.
 */
public class SimpleNodeUsage implements NodeUsage {
    private final int start;
    private final int end;
    private final String description;
    private final String uri;
    private final NodeType nodeType;

    public SimpleNodeUsage(int start, int end, String description, String uri, NodeType nodeType) {
        this.start = start;
        this.end = end;
        this.description = description;
        this.uri = uri;
        this.nodeType = nodeType;
    }

    public SimpleNodeUsage(IdentifierNode identifierNode, String description, String uri, NodeType nodeType) {
        this.nodeType = nodeType;
        this.start = identifierNode.getLocation()
            .getStart()
            .getAbsolutePosition(new TextInfo(identifierNode.getIdentifier()));
        this.end = identifierNode.getLocation()
            .getEnd()
            .getAbsolutePosition(new TextInfo(identifierNode.getIdentifier())) - 1;
        this.description = description;
        this.uri = uri;
    }

    @Override
    public int getStart() {
        return start;
    }

    @Override
    public int getEnd() {
        return end;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public NodeType getNodeType() {
        return nodeType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SimpleNodeUsage that = (SimpleNodeUsage) o;
        return start == that.start && end == that.end && Objects.equals(description, that.description) && Objects
            .equals(uri, that.uri) && nodeType == that.nodeType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, description, uri, nodeType);
    }
}

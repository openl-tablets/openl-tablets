package org.openl.binding.impl;

import java.util.Objects;

import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
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
    private final IOpenClass type;

    public SimpleNodeUsage(int start, int end, String description, String uri, NodeType nodeType) {
        this(start, end, description, uri, null, nodeType);
    }

    /**
     * @param end the ending index position, exclusive
     */
    public SimpleNodeUsage(int start, int end, String description, String uri, IOpenClass type, NodeType nodeType) {
        this.start = start;
        this.end = end;
        this.description = description;
        this.uri = uri;
        this.nodeType = nodeType;
        this.type = extractRootType(type);
    }

    public SimpleNodeUsage(IdentifierNode identifierNode, String description, String uri, NodeType nodeType) {
        this(identifierNode, description, uri, null, nodeType);
    }

    public SimpleNodeUsage(IdentifierNode identifierNode,
            String description,
            String uri,
            IOpenClass type,
            NodeType nodeType) {
        this.nodeType = nodeType;
        this.start = identifierNode.getLocation()
            .getStart()
            .getAbsolutePosition(new TextInfo(identifierNode.getIdentifier()));
        this.end = identifierNode.getLocation()
            .getEnd()
            .getAbsolutePosition(new TextInfo(identifierNode.getIdentifier()));
        this.description = description;
        this.uri = uri;
        this.type = extractRootType(type);
    }

    private static IOpenClass extractRootType(IOpenClass type) {
        IOpenClass t = type;
        while (t != null && t.isArray()) {
            t = t.getComponentClass();
        }
        return t;
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

    public IOpenClass getType() {
        return type;
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

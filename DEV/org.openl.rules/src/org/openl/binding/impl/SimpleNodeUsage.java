package org.openl.binding.impl;

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

    public SimpleNodeUsage(int start, int end, String description, String uri) {
        this.start = start;
        this.end = end;
        this.description = description;
        this.uri = uri;
    }

    public SimpleNodeUsage(IdentifierNode identifierNode, String description, String uri) {
        this.start = identifierNode.getLocation().getStart().getAbsolutePosition(new TextInfo(identifierNode.getIdentifier()));
        this.end = identifierNode.getLocation().getEnd().getAbsolutePosition(new TextInfo(identifierNode.getIdentifier())) - 1;
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
}

package org.openl.binding.impl;

import org.openl.syntax.impl.IdentifierNode;
import org.openl.util.text.TextInfo;

/**
 * @author nsamatov.
 */
public class SimpleNodeUsage implements NodeUsage {
    private final IdentifierNode identifierNode;
    private final String description;
    private final String uri;

    public SimpleNodeUsage(IdentifierNode identifierNode, String description, String uri) {
        this.identifierNode = identifierNode;
        this.description = description;
        this.uri = uri;
    }

    @Override
    public int getStart() {
        return identifierNode.getLocation().getStart().getAbsolutePosition(new TextInfo(identifierNode.getIdentifier()));
    }

    @Override
    public int getEnd() {
        return identifierNode.getLocation().getEnd().getAbsolutePosition(new TextInfo(identifierNode.getIdentifier())) - 1;
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

/*
 * Created on May 13, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.StringPool;
import org.openl.util.text.ILocation;

/**
 * @author snshor
 *
 */
public class IdentifierNode extends TerminalNode {

    private final String identifier;
    private final String originalIdentifier;

    public IdentifierNode(String type, ILocation location, String identifier, IOpenSourceCodeModule module) {
        super(type, location, module);
        String x = identifier.replaceAll("`", "");
        this.originalIdentifier = !x.equals(identifier) ? StringPool.intern(identifier) : null;
        this.identifier = StringPool.intern(x);
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getOriginalIdentifier() {
        return originalIdentifier != null ? originalIdentifier : getIdentifier();
    }

    @Override
    public String getText() {
        return identifier;
    }

    @Override
    protected void printMySelf(int level, StringBuilder buf) {
        super.printMySelf(level, buf);
        buf.append("=").append(identifier);
    }

}

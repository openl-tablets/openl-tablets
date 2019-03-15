package org.openl.syntax.impl;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.TextInterval;

/**
 * @author snshor
 *
 */
public class LiteralNode extends TerminalNode {
    
    private String image;

    LiteralNode(String type, TextInterval location, String image, IOpenSourceCodeModule module) {
        super(type, location, module);
        this.image = image;
    }

    @Override
    public String getText() {
        return image;
    }

    @Override
    protected void printMySelf(int level, StringBuilder buf) {
        super.printMySelf(level, buf);
        buf.append("=").append(image);
    }

}

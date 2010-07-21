/*
 * Created on May 13, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.TextInterval;

/**
 * @author snshor
 *
 */
public class LiteralNode extends TerminalNode {
    
    private String image;

    public LiteralNode(String type, TextInterval location, String image, IOpenSourceCodeModule module) {
        super(type, location, module);
        this.image = image;
    }

    public String getImage() {
        return image;
    }

    @Override
    protected void printMySelf(int level, StringBuffer buf) {
        super.printMySelf(level, buf);
        buf.append("=" + image);
    }

}

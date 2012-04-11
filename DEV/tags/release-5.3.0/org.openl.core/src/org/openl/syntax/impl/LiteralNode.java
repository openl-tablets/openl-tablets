/*
 * Created on May 13, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import org.openl.IOpenSourceCodeModule;
import org.openl.util.text.TextInterval;

/**
 * @author snshor
 *
 */
public class LiteralNode extends TerminalNode {
    String image;

    public LiteralNode(String type, TextInterval pos, String image, IOpenSourceCodeModule module) {
        super(type, pos, module);
        this.image = image;
    }

    /**
     * @return
     */
    public String getImage() {
        return image;
    }

    @Override
    protected void printMySelf(int level, StringBuffer buf) {
        super.printMySelf(level, buf);
        buf.append("=" + image);
    }

}

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
 */
public class EmptyNode extends TerminalNode {

    public EmptyNode(String type, TextInterval pos, IOpenSourceCodeModule module) {
        super(type, pos, module);
    }

}

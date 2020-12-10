/*
 * Created on Oct 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.syntax;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.impl.TerminalNode;
import org.openl.util.text.ILocation;

/**
 * @author snshor
 *
 */
public class OpenlSyntaxNode extends TerminalNode {

    private final String openlName;

    public OpenlSyntaxNode(String openlName, ILocation location, IOpenSourceCodeModule module) {
        super(XlsNodeTypes.XLS_OPENL.toString(), location, module);

        this.openlName = openlName;
    }

    public String getOpenlName() {
        return openlName;
    }

}

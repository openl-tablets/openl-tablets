/*
 * Created on Jun 2, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import org.openl.syntax.ISyntaxNode;

/**
 * @author snshor
 */

public interface INodeBinderFactory {

    INodeBinder getNodeBinder(ISyntaxNode node);

}

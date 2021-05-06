/*
 * Created on Jul 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.grammar;

import java.io.Reader;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;

/**
 * @author snshor
 *
 */
public interface IGrammar {

    SyntaxNodeException getError();

    ISyntaxNode getTopNode();

    void parseAsMethod(Reader reader);

    void parseAsMethodHeader(Reader reader);

    void parseAsModule(Reader reader);

    void parseAsType(Reader reader);

    void parseAsParamDeclaration(Reader reader);

    void setModule(IOpenSourceCodeModule module);

}

/*
 * Created on Jul 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax;

import java.io.Reader;

import org.openl.IOpenSourceCodeModule;

/**
 * @author snshor
 *
 */
public interface IGrammar {
    public ISyntaxError[] getErrors();

    public ISyntaxNode getTopNode();

    public void parse(Reader characterStream, String parseType);

    public void parseAsMethod(Reader r);

    public void parseAsMethodHeader(Reader r);

    public void parseAsModule(Reader r);

    /**
     * @param reader
     */
    public void parseAsType(Reader reader);

    public void setModule(IOpenSourceCodeModule module);

}

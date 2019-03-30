/*
 * Created on May 14, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.grammar;

import java.io.Reader;

import org.openl.syntax.grammar.impl.Grammar;
import org.openl.util.text.IPosition;

/**
 * This class is the base for all JavaCC v3.0 and compatible grammars.
 * 
 * @author snshor
 */
public abstract class JavaCC30Grammar extends Grammar {

    @Override
    public void parse(Reader r, String parseType) {

        ReInit(r);
        parseTopNode(parseType);
    }

    @Override
    public void parseAsMethod(Reader r) {

        ReInit(r);
        parseTopNode("method.body");
    }

    @Override
    public void parseAsMethodHeader(Reader r) {

        ReInit(r);
        parseTopNode("method.header");
    }

    @Override
    public void parseAsModule(Reader r) {

        ReInit(r);
        parseTopNode("module");
    }

    @Override
    public void parseAsType(Reader reader) {

        ReInit(reader);
        parseTopNode("type");
    }

    protected IPosition pos(int line, int col) {

        return new JavaCC30Position(line, col);
    }

    public abstract void parseTopNode(String rootType);

    public abstract void ReInit(Reader r);

}

/*
 * Created on May 9, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.grammar.impl;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.grammar.IGrammar;
import org.openl.syntax.impl.SyntaxTreeBuilder;

/**
 * @author snshor
 * 
 */
public abstract class Grammar implements IGrammar {

    protected SyntaxTreeBuilder syntaxBuilder = new SyntaxTreeBuilder();

    /*
     * (non-Javadoc)
     * @see org.openl.syntax.IGrammar#getErrors()
     */
    @Override
    public SyntaxNodeException[] getErrors() {

        return syntaxBuilder.getSyntaxErrors();
    }

    /**
     * @return
     */
    public SyntaxTreeBuilder getSyntaxTreeBuilder() {

        return syntaxBuilder;
    }

    /*
     * (non-Javadoc)
     * @see org.openl.syntax.IGrammar#getTopNode()
     */
    @Override
    public ISyntaxNode getTopNode() {

        return syntaxBuilder.getTopnode();
    }

    /*
     * (non-Javadoc)
     * @see org.openl.syntax.IGrammar#setModule(org.openl.IOpenSourceCodeModule)
     */
    @Override
    public void setModule(IOpenSourceCodeModule module) {

        syntaxBuilder.setModule(module);
    }

}

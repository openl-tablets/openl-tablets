/*
 * Created on May 9, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import org.openl.syntax.grammar.IGrammarFactory;

/**
 * Default implementation of {@link AParser}.
 *
 * @author snshor
 */
public class Parser extends AParser {

    private IGrammarFactory grammarFactory;

    public Parser(IGrammarFactory grammarFactory) {

        this.grammarFactory = grammarFactory;
    }

    @Override
    public IGrammarFactory getGrammarFactory() {

        return grammarFactory;
    }
}

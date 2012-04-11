/*
 * Created on May 9, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import org.openl.syntax.IGrammarFactory;

/**
 * @author snshor
 */

public class Parser extends AParser {
    IGrammarFactory grammarFactory;

    public Parser(IGrammarFactory gf) {
        grammarFactory = gf;
    }

    /**
     * @return
     */
    @Override
    public IGrammarFactory getGrammarFactory() {
        return grammarFactory;
    }

}

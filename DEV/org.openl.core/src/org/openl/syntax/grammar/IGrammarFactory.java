/*
 * Created on Jun 2, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.grammar;

/**
 * Interface that provides contract for all grammar factories implementations.
 *
 * @author snshor
 *
 */
public interface IGrammarFactory {

    /**
     * Gets grammar that will be used during parse phase.
     *
     * @return {@link IGrammar} instance
     */
    IGrammar getGrammar();
}

/*
 * Created on Jun 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import org.openl.binding.ICastFactory;
import org.openl.binding.INameSpacedMethodFactory;
import org.openl.binding.INameSpacedTypeFactory;
import org.openl.binding.INameSpacedVarFactory;
import org.openl.binding.INodeBinderFactory;
import org.openl.syntax.grammar.IGrammarFactory;

/**
 * @author snshor
 *
 */
public interface IOpenLConfiguration extends IGrammarFactory, INodeBinderFactory, INameSpacedMethodFactory,
        ICastFactory, INameSpacedVarFactory, INameSpacedTypeFactory {

    void addOpenFactory(IOpenFactoryConfiguration opfc);

    IConfigurableResourceContext getConfigurationContext();

}

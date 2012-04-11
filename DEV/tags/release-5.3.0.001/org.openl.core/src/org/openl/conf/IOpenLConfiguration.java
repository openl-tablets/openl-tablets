/*
 * Created on Jun 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import org.openl.OpenConfigurationException;
import org.openl.binding.ICastFactory;
import org.openl.binding.INameSpacedMethodFactory;
import org.openl.binding.INameSpacedTypeFactory;
import org.openl.binding.INameSpacedVarFactory;
import org.openl.binding.INodeBinderFactory;
import org.openl.syntax.IGrammarFactory;
import org.openl.types.IOpenFactory;

/**
 * @author snshor
 *
 */
public interface IOpenLConfiguration extends IGrammarFactory, INodeBinderFactory, INameSpacedMethodFactory,
        ICastFactory, INameSpacedVarFactory, INameSpacedTypeFactory {

    public void addOpenFactory(IOpenFactoryConfiguration opfc) throws OpenConfigurationException;

    public IConfigurableResourceContext getConfigurationContext();

    public IOpenFactory getOpenFactory(String name);

}

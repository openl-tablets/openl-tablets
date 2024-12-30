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

/**
 * @author snshor
 */
public interface IOpenLConfiguration extends INodeBinderFactory, INameSpacedMethodFactory, ICastFactory, INameSpacedVarFactory, INameSpacedTypeFactory {

}

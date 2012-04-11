/*
 * Created on May 9, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types;

import org.openl.binding.BindingDependencies;
import org.openl.meta.IMetaInfo;
import org.openl.syntax.ISyntaxNode;

/**
 * @author snshor
 *
 */
public interface IMemberMetaInfo extends IMetaInfo {
    BindingDependencies getDependencies();

    ISyntaxNode getSyntaxNode();
}

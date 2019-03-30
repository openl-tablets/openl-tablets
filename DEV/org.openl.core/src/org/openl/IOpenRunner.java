/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl;

import org.openl.binding.IBoundMethodNode;
import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public interface IOpenRunner {

    Object run(IBoundMethodNode node, Object[] params) throws OpenLRuntimeException;

    Object run(IBoundMethodNode node, Object[] params, IRuntimeEnv env) throws OpenLRuntimeException;

    /**
     * Runs a single expression node. An implementation should be optimized to efficiently run single expressions or
     * formulas. The expressions do not have local variables, which allows for more efficient allocation (actually, to
     * skip it altogether) of a local frame
     * 
     * @since 5.9.4
     * 
     */

    Object runExpression(IBoundNode expressionNode, Object[] params, IRuntimeEnv env);

}

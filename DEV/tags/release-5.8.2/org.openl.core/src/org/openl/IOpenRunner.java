/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl;

import org.openl.binding.IBoundMethodNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public interface IOpenRunner {

    Object run(IBoundMethodNode node, Object[] params) throws OpenLRuntimeException;

    Object run(IBoundMethodNode node, Object[] params, IRuntimeEnv env) throws OpenLRuntimeException;

}

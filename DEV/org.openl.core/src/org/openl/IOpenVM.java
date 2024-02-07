/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl;

import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 */
public interface IOpenVM {

    IOpenRunner getRunner();

    IRuntimeEnv getRuntimeEnv();
}

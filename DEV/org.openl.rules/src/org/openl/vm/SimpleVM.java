/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.vm;

import org.openl.IOpenRunner;
import org.openl.IOpenVM;

/**
 * @author snshor
 */
public class SimpleVM implements IOpenVM {

    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenVM#run(org.openl.binding.IBoundCode)
     */
    @Override
    public IOpenRunner getRunner() {
        return SimpleRunner.SIMPLE_RUNNER;
    }

    @Override
    public IRuntimeEnv getRuntimeEnv() {
        return new SimpleRuntimeEnv();
    }

}

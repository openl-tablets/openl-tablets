/**
 * Created Jul 7, 2007
 */
package org.openl.types.impl;

import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 * This interface can be used by different implementations of open methods where
 * invoke() method have significant "preparation stage" overhead vs actual
 * performance execution. If implemented the method benchmark() should implement
 * the internal benchmarking style.
 *
 * The example of such a method would be the TestMethod in OpenL Tablets where a
 * significant portion of the invoke() is spent on the preparation the data for
 * internal tested method invoke() and this would skew the results, because we
 * actually interested in benchmarking of the tested method.
 *
 */

public interface IBenchmarkableMethod extends IOpenMethod {

    String getBenchmarkName();

    Object invokeBenchmark(Object target, Object[] params, IRuntimeEnv env, long ntimes);

    int nUnitRuns();

    String[] unitName();
}

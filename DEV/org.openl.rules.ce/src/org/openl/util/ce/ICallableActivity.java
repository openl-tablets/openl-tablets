package org.openl.util.ce;

import java.util.concurrent.Callable;

public interface ICallableActivity<T> extends IActivity,Callable<T> {

}

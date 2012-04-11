/**
 *
 */
package org.openl.rules.ruleservice.loader;

import java.util.EventListener;

/**
 * @author Sergey Zyrianov
 *
 */
public interface LoadingListener extends EventListener {

    public void onAfterLoading(LoadingEventObject loadedDeployment);

    public void onBeforeLoading(LoadingEventObject loadingDeployment);

}

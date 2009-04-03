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

    public void onBeforeLoading(LoadingEventObject loadingDeployment);

    public void onAfterLoading(LoadingEventObject loadedDeployment);

}
